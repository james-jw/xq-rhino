package org.jw.basex.js.rhino;

import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.XQFunction;
import org.basex.query.value.Value;
import org.basex.query.value.ValueBuilder;
import org.basex.util.Util;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
/*
 * Implements the Mozilla Javascript Function interface to allow 
 * XQuery functions to be passed into Javascript and called.
 * 
 * @author James Wright
 * @email james.jw hotmail com
 * @date 10/24/15
 * @license: MIT
 */
public class JsCallback implements Function {
	
	XQFunction func;
	QueryContext xqCx;
	Scriptable scope;
	
	public JsCallback(XQFunction funcIn, QueryContext xqContext) {
		func = funcIn;
		xqCx = xqContext;	
	}
	
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		Util.notExpected("Cannot construct JsCallback.", this);
		return null;
	}
	
	ValueBuilder vb = new ValueBuilder();
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
	   Object out = null;
	   try {
		  Value[] arguments = Js.xqValues(args, xqCx);
		  Value result = func.invokeValue(xqCx, null, arguments);
		  out = Js.jsValue(result, xqCx);
	   } catch (QueryException e) {
		   Util.notExpected("Failed to process XQFunction invocation from javascript context.", e);
	   }
	   
	   return out;
	}

	public String getClassName() {
		return this.getClass().getSimpleName();
	}

	public Object get(String name, Scriptable start) {
		Object out = null;
		if(name.equals("call")) {
			out = this;
		}
		return out;
	}

	public Object get(int index, Scriptable start) {
		Util.notExpected("Cannot get from JsCallback.", this);
		return null;
	}

	public boolean has(String name, Scriptable start) {
		return false;
	}

	public boolean has(int index, Scriptable start) {
		return false;
	}

	public void put(String name, Scriptable start, Object value) {
		Util.notExpected("Cannot put on JsCallback.", this);
	}

	public void put(int index, Scriptable start, Object value) {
		Util.notExpected("Cannot put on JsCallback.", this);
	}

	public void delete(String name) {
		Util.notExpected("Cannot delete from JsCallback.", this);
	}

	public void delete(int index) {
		Util.notExpected("Cannot delete from JsCallback.", this);
	}

	public Scriptable getPrototype() {
		return ScriptableObject.getClassPrototype(scope, "Object");
	}

	public void setPrototype(Scriptable prototype) {
		Util.notExpected("Cannot set prototype on JsCallback.", this);
	}

	public Scriptable getParentScope() {
		return scope;
	}

	public void setParentScope(Scriptable parent) {
		scope = parent;
	}

	public Object[] getIds() {
		return new Object[0];
	}

	public Object getDefaultValue(Class<?> hint) {
		return "[object JsCallback]";
	}

	public boolean hasInstance(Scriptable instance) {
        return false;
	}
	
}
