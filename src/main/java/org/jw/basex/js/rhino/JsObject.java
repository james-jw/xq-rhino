package org.jw.basex.js.rhino;

import static org.basex.query.QueryError.castError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.Expr;
import org.basex.query.expr.XQFunction;
import org.basex.query.util.list.AnnList;
import org.basex.query.value.Value;
import org.basex.query.value.ValueBuilder;
import org.basex.query.value.item.Bln;
import org.basex.query.value.item.FItem;
import org.basex.query.value.item.Item;
import org.basex.query.value.item.QNm;
import org.basex.query.value.item.Str;
import org.basex.query.value.node.FElem;
import org.basex.query.value.type.FuncType;
import org.basex.query.value.type.SeqType;
import org.basex.query.var.VarScope;
import org.basex.util.InputInfo;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/*
 * Represents a Javascript object for use within XQuery code. Allows XQuery to 
 * transport and operate on Javascript contructs.
 * 
 * @author James Wright * 
 * @email james.jw hotmail com
 * @date 10/24/15
 * @license MIT
 * 
 */
public class JsObject extends FItem implements XQFunction {

	private Scriptable scope;
	private Scriptable rootScope;
	private Context cx;
	private QueryContext xqCx;
	private Function self;
	private Scriptable thisObj;
	private int arity;
	
	public Bln isFunction() {
		return Bln.get(self != null);
	}
	
	public Bln isArray() {
		return Bln.get(scope instanceof NativeArray);
	}
	
	public JsObject(Context cxIn, Scriptable scopeIn, Scriptable rootScopeIn, QueryContext xqCxIn) {
	    super(SeqType.ANY_FUN, new AnnList());
		scope = scopeIn;
		if(scopeIn instanceof BaseFunction) {
			BaseFunction n = (BaseFunction)scopeIn;
			self = n;
			arity = n.getArity();
		} else if(scopeIn instanceof JsCallback) {
			JsCallback callback = (JsCallback)scopeIn;
			self = callback;
			arity = callback.func.arity();			
		}  else {
			arity = 1;
		}
		
		rootScope = rootScopeIn;
		xqCx = xqCxIn;
	}
	
	public JsObject(Context cxIn, Scriptable scopeIn, Scriptable rootScopeIn, Scriptable thisObjIn, QueryContext xqCxIn) {
		this(cxIn, scopeIn, rootScopeIn, xqCxIn);
		thisObj = thisObjIn;
	}
	
	public QNm argName(int arg0) {
		return new QNm("property", "");
	}

	public int arity() {
		return arity;
	}

	public QNm funcName() {
		return new QNm(self == null ? "object" : "function", "javascript");
	}

	public FuncType funcType() {
		return FuncType.get(SeqType.ITEM_ZM, SeqType.STR_ZM, SeqType.ITEM_ZM);
	}

	public Expr inlineExpr(Expr[] arg0, QueryContext arg1, VarScope arg2, InputInfo arg3) throws QueryException {
		return null;
	}

	public Item invItem(QueryContext qc, InputInfo ii, Value... args) throws QueryException {
		return invValue(qc, ii, args).item(qc, ii);
	}
	
	private Object getFromPath(List<String> path, Scriptable scope, boolean recursive) {
		String key = path.remove(0);
		Object out = recursive == false ? 
				scope.get(key, scope) : 
				ScriptableObject.getProperty(scope, key);
				
		if(path.isEmpty()) {
			return out;
		}
		
		return getFromPath(path, (Scriptable) out, recursive);
	}
	
	public Object getFromPath(String pathStr, Scriptable scope, boolean recursive) {
		List<String> path = new ArrayList<String>();
		path.addAll(Arrays.asList(pathStr.split("[.?]")));
		if(path.isEmpty()) {
			path.add(pathStr);
		}
		return getFromPath(path, scope, recursive);		
	}

	public Value invValue(QueryContext qc, InputInfo ii, Value... args) throws QueryException {
		
		Object jsValue = null;
		cx = Context.enter();
		try {
			if(self == null && args[0] instanceof Str) {
				jsValue = this.getFromPath(((Str)args[0]).toJava(), scope, false);
			} else if(self != null){
				Object[] arguments = Js.jsValues(args, qc);
				if(thisObj != null) {
					jsValue = this.self.call(cx, rootScope, thisObj, arguments);
				} else {
					jsValue = this.self.call(cx, rootScope, self, arguments);
				}
			}
		} catch (Exception e) {
			throw new QueryException(e);
		} finally {
			Context.exit();
		}
		
		return Js.xqValue(jsValue, cx, rootScope, null, qc);
	}
	
	public Object call(Context cx, Scriptable rootScope, Object... jsArguments) {
		Function constructor = (Function) ScriptableObject.getFunctionPrototype(scope);
		return constructor.call(cx, rootScope, scope, jsArguments);
	}
	
	public Scriptable constructor(Context cx, Scriptable rootScope, Object... jsArguments) {
		Function constructor = (Function) ScriptableObject.getFunctionPrototype(scope);
	    return constructor.construct(cx, rootScope, jsArguments);
	}

	public Value keys(Bln recursive) throws QueryException {
		ValueBuilder vb = new ValueBuilder();
		Object[] keys = recursive == Bln.FALSE ? scope.getIds() :ScriptableObject.getPropertyIds(scope);
		for(Object key : keys) {
			vb.add(Js.xqValue(key, cx, scope, null, xqCx));
		}
		return vb.value();
	}
	
    public Value keys() throws QueryException {
    	return keys(Bln.FALSE);
    }

	public int stackFrameSize() {
		return 0;
	}

	@Override
	public String toString() {
		cx = Context.enter();
		try {
			Str json = Js.pretty(cx, scope);
			return json.toJava();
		} catch (Exception e) {
			return e.getMessage();
		} finally {
			Context.exit();
		}
	}  

	  @Override
	  public FItem coerceTo(FuncType ft, QueryContext qc, InputInfo ii, boolean opt)
	      throws QueryException {
	    if(instanceOf(ft)) return this;
	    throw castError(ii, this, ft);
	  }

	@Override
	public void plan(FElem arg0) {

	}

	@Override
	public Object toJava() throws QueryException {
		return this;
	}

	public Scriptable getScope() {
		return scope;
	}	
}
