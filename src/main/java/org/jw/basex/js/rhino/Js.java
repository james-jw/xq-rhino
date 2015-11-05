package org.jw.basex.js.rhino;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.QueryModule;
import org.basex.query.expr.XQFunction;
import org.basex.query.value.Value;
import org.basex.query.value.ValueBuilder;
import org.basex.query.value.array.Array;
import org.basex.query.value.item.Bln;
import org.basex.query.value.item.Dat;
import org.basex.query.value.item.Dbl;
import org.basex.query.value.item.Dtm;
import org.basex.query.value.item.FItem;
import org.basex.query.value.item.Int;
import org.basex.query.value.item.Item;
import org.basex.query.value.item.Str;
import org.basex.query.value.item.Tim;
import org.basex.query.value.map.Map;
import org.basex.util.Token;
import org.basex.util.Util;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.tools.shell.Global;

import com.google.gson.Gson;

/*
 * Module for loading and interacting with Javascript code in XQuery
 * leveraging the great work of Moazilla Rhino and commonJS framework
 * 
 * @author James Wright
 * @email james.jw hotmail com
 * @date 10/24/15
 * @license MIT
 */
public class Js extends QueryModule {
	
	static int dynamicCount = 0;
	static Global scope;
	public static JsObject require;
	
	public Js() {
	   if(scope == null) {
		   Context cx = Context.enter();
		   // Initialize the standard objects (Object, Function, etc.)
		   // This must be done before scripts can be executed.
		   cx.setLanguageVersion(Context.VERSION_1_7);	   
		   scope = new Global();
	       
	       // initialize Object, Array, Number, Date, etc.
		   scope.initStandardObjects(cx, false);
	       
		   Context.exit();
	   }
	}
	
	public Value require(Value sourcePaths) throws QueryException {
       Context cx = Context.enter();
       try {
    	   if(require == null) {
			   // install the global require function for all modules
		       List<String> paths = new ArrayList<String>((int) sourcePaths.size());
		       for(Value path : sourcePaths) {
		    	   Path uri = Paths.get((String)path.toJava());
		    	   paths.add(uri.toUri().toString());
		       }
			   Require reqScript = scope.installRequire(cx, paths, false);
			   require = new JsObject(cx, reqScript, scope, queryContext);
    	   }
		   return require;
		} catch (Exception e) {
		   throw new QueryException(e);
		} finally {
			Context.exit();
		}
	}
	
	static FItem _mapFunction;
	/*
	 * Returns a commonJs object for requiring modules
	 */
	public Value require(Value sourcePaths, FItem mapFunction) throws QueryException {
		_mapFunction = mapFunction;
		return require(sourcePaths);
	}
	
	public Value toValue(Object valueIn) {		
		return valueIn instanceof JsObject ? (JsObject)valueIn : Str.get((String)valueIn);
	}
	
	JsObject jsScope;
	private String invalidCastMessage = "Either key not found or path provided is not a Javascript object (JsObject)";
	
	private Value requireInternal(Str script, String source) throws QueryException {
	   Context cx = Context.enter();
	   try {
		   cx.evaluateString(scope, script.toJava() , source, 1, null);		 
		   return new JsObject(cx, scope, null, queryContext);
	   } finally {
		   Context.exit();
	   }
	}
	
	public Value attach(Str name, Str scriptIn) throws QueryException {
		Context cx = Context.enter();
		try {
			JsObject script = (JsObject) requireInternal(scriptIn, name.toJava());
			return Js.xqValue(script.getFromPath(name.toJava(), script.getScope(), false), cx, script.getScope(), null, queryContext);
		} finally {
			Context.exit();
		}
	}
	
	public Value attach(Str script) throws QueryException {
		return requireInternal(script, "require-dynamic" + dynamicCount++);
	}
	
	public Value construct(Value item, Object... args) throws QueryException {
		Context cx = Context.enter();
		Value out = null;
		try {
			if(item instanceof JsObject) {
				Scriptable newItem = ((JsObject)item).getScope();
			    if(newItem.getClassName().equals("Object") == false) {
					newItem = ((Function)newItem).construct(cx, scope, args);
					return Js.xqValue(newItem, cx, scope, null, queryContext);
			    }				
			}
			
			if(item instanceof Str) {
				String temp = "__xq_rhino_temp_var__";
				Object result = cx.evaluateString(scope, temp + " = " + (String)item.toJava(), "dynamic", 1, null);		 
				scope.delete(temp);
				out = Js.xqValue(result, cx, scope, null, queryContext);
			} else { // Convert
				Object jsValue = Js.jsValue(item, queryContext);
				out = Js.xqValue(jsValue, cx, scope, null, queryContext);
			}
		} catch (Exception e) {
			Util.notExpected("Faild to construct object. Either not function or not string.", e);
		} finally {
			Context.exit();
		}
		
		return out;
	}
	
	public Value construct(Value item) throws QueryException {
		return construct(item, new Object[0]);
	}
	
	public Bln isFunction(Value item) {
		return Bln.get(item instanceof JsObject && ((JsObject)item).isFunction() == Bln.TRUE);
	}
	
	public Bln isMap(Value item) {
		return Bln.get(item instanceof JsObject && ((JsObject)item).isFunction() == Bln.FALSE);
	}
	
	public Bln isArray(Value item) {
		return Bln.get(item instanceof JsObject && ((JsObject)item).isArray() == Bln.TRUE);
	}
	
	public Bln isObject(Value item) {
		return Bln.get(item instanceof JsObject);
	}
	
	public Str typeOf(Value item) throws QueryException {
		if(item instanceof JsObject) {
			return Str.get(((JsObject)item).getScope().getClassName());
		}
		
		throw new QueryException("Invalid use of type-of. Item provided not a javascript object: " + item);
	}
	
	/*
	 * Returns the property at the specified path
	 * 
	 * If recursive is true, it will search the prototype chain
	 */
	public Value get(Value jsObject, Value path, Bln recursive) throws QueryException {
		Context cx = Context.enter();
		Scriptable parent = null;
		try {
			Object out = jsObject;
			if(jsObject instanceof Map) {
				Map map = (Map)jsObject;
				jsObject = map.get(selfProperty(), null);
			}
			
			if(jsObject instanceof JsObject) {
				JsObject obj = (JsObject)jsObject;
				if(path instanceof Str) {
				   out = obj.getFromPath(((Str)path).toJava(), obj.getScope(), recursive.toJava());
				   parent = obj.getScope();
				} else if (path instanceof Int) {
				   BigInteger index = (BigInteger) ((Int)path).toJava();
				   out = obj.getScope().get(index.intValue(), obj.getScope());
				}
				
				return Js.xqValue(out, cx, scope, parent, queryContext);
			}
		} catch (Exception e) {
			Util.notExpected("Failed to get key " + path, e);
		} finally {
			Context.exit();
		}
		
		throw new QueryException("Error: " + invalidCastMessage);
	}
	
	public Value get(Value jsObject, Value path) throws QueryException {
		return get(jsObject, path, Bln.FALSE);
	}
	
	public Value remove(Value jsObject, Value path) throws QueryException {
		Context cx = Context.enter();
		try {
			if(jsObject instanceof Map) {
				Map map = (Map)jsObject;
				jsObject = map.get(selfProperty(), null);
			}
			
			if(jsObject instanceof JsObject) {
				JsObject obj = (JsObject)jsObject;
				Scriptable item = obj.getScope();
				if(path instanceof Str) {
					item.delete(((Str)path).toJava());
				} else if (path instanceof Int) {
					BigInteger index = (BigInteger) ((Int)path).toJava();
					item.delete(index.intValue());
				}
				
				return Js.xqValue(item, cx, scope, null, queryContext);
			}
		} catch (Exception e) {
			Util.notExpected("Failed to delete key " + path, e);
		} finally {
			Context.exit();
		}
		
		throw new QueryException("Error: Removing value failed. " + invalidCastMessage);
	}
	
	/* 
	 * Property to store a JsObject at when converted to a map
	 */
	static Str selfPropName = Str.get("__xq_this__");
	public static Str selfProperty() {
		return selfPropName;
	}
	
	public Value put(Value jsObject, Value path, Value valueIn) throws QueryException {
		Context cx = Context.enter();
		try {
			if(jsObject instanceof Map) {
				Map map = (Map)jsObject;
				jsObject = map.get(selfProperty(), null);
			}
			
			if(jsObject instanceof JsObject) {
				JsObject obj = (JsObject)jsObject;
				Scriptable item = obj.getScope();
				Object value = Js.jsValue(valueIn, queryContext);
				if(path instanceof Str) {
					item.put(((Str)path).toJava(), item, value);
				} else if (path instanceof Int) {
					BigInteger index = (BigInteger) ((Int)path).toJava();
					item.put(index.intValue(), item, value);
				}
				
				return Js.xqValue(item, cx, scope, null, queryContext);
			}
		} catch (Exception e) {
			Util.notExpected("Failed to put key " + path, e);
		} finally {
			Context.exit();
		}
		
		throw new QueryException("Error: Putting value failed. " + invalidCastMessage );
	}
	
	public Value keys(Value jsObject, Bln recursive) throws Exception {
		try {
			if(jsObject instanceof JsObject) {
				JsObject obj = (JsObject)jsObject;
				return obj.keys(recursive);
			}
		} catch (Exception e) {
			Util.notExpected("Error retrieving key from javascript object: " + jsObject.toJava().toString(), e);
		}
		
		throw new QueryException("Error: retrieving keys failed. " + invalidCastMessage);
	}
	
	public Value keys(Value jsObject) throws Exception {
		return keys(jsObject, Bln.FALSE);
	}

	/* Converts a date into ISO time format for interopability between
	 * java, xquery and javascript
	 */
	private static Dtm toXqDate(Date time) throws QueryException {
		String[] timeStr = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(time).split("T");

		byte[] dateToken = Token.token(timeStr[0]);
		byte[] timeToken = Token.token(timeStr[1]);
		Tim xqTim = new Tim(timeToken, null);
		Dat xqDat = new Dat(dateToken, null);
		
		 Dtm dateOut = new Dtm(xqDat, xqTim, null);
		 return dateOut;
	}
	
	public static Value xqValue(Object jsValue, Context cxIn, Scriptable scopeIn, Scriptable parent, QueryContext xqCx) throws QueryException {
		ValueBuilder vb = new ValueBuilder();
		
		if(jsValue instanceof Scriptable) {
			Scriptable sV = (Scriptable)jsValue;
			
		    if(sV instanceof NativeArray) {
				vb.add(Array.from(Js.xqValues(((NativeArray)sV).toArray(), xqCx)));
			} else if(sV.getClassName().equals("Date")) {	
				vb.add(toXqDate((Date) Context.jsToJava(sV, Date.class)));
			} else if(jsValue instanceof NativeJavaObject) {
				Object jV = ((NativeJavaObject)jsValue).unwrap();
				if(jV instanceof XMLGregorianCalendar) {
					XMLGregorianCalendar value = (XMLGregorianCalendar)jV;
					Date time = value.toGregorianCalendar().getTime();
					vb.add(toXqDate(time));
				} else {
					throw new QueryException("Unable to convert unknown java type: " + jV.getClass().getSimpleName());
				}
			} else {
				Value value = new JsObject(cxIn, sV , scopeIn, parent, xqCx);
				if(_mapFunction != null && ((Scriptable)jsValue).getClassName().equals("Object")) {
					vb.add(_mapFunction.invokeValue(xqCx, null, value));
				} else {
					vb.add(value);
				}
			}
		} else if(jsValue instanceof Integer) {
			vb.add(Int.get(((Integer) jsValue).longValue()));
		} else if(jsValue instanceof Double) {
		    vb.add(Dbl.get((Double)jsValue));	
		} else if(jsValue instanceof Boolean) {
			vb.add(Bln.get((Boolean)jsValue));
		} else if(jsValue instanceof String) {
			vb.add(Str.get(jsValue.toString()));
		} else if(jsValue instanceof Object[]) {
		    vb.add(Array.from(Js.xqValues((Object[]) jsValue, xqCx)));
		} else if(jsValue instanceof Undefined || jsValue == null) {
			// do nothing.
		} else {
			throw new QueryException("Unknown type: " + jsValue.getClass().getSimpleName());
		}
	
		return vb.value();
	}
	
	public static Object jsValue(Value xqValue, QueryContext xqCx) throws QueryException {
		Context cx = Context.enter();
		try {
		Object out = null;		
		if(xqValue instanceof JsObject) {
			out = (Scriptable) ((JsObject)xqValue).getScope();
		} else if(xqValue instanceof Map) {
			java.util.Map map = (java.util.Map) jsMap((Map)xqValue, xqCx);
			Gson gson = new Gson(); 
			
			@SuppressWarnings("unchecked")
			java.util.Map<String, Function> functions = (java.util.Map<String, Function>) map.remove("_x_q__callbacks__");
			map.remove(Js.selfPropName.toJava());
			
			// Convert the none
			String json = gson.toJson(map);
			Scriptable obj = (Scriptable) cx.evaluateString(scope, "_x_q__rhino_" + dynamicCount++ + " = " + (json.equals("null") ? "{};" : json), "fromMap", 1, null);
			for(Entry<String, Function> func : functions.entrySet()) {
				ScriptableObject.putProperty(obj, func.getKey(), func.getValue());
			}
			obj.delete("_x_q__callbacks__");
			
			out = obj;
		} else if(xqValue instanceof Array) {
			out = jsArray((Array)xqValue, xqCx);
		} else if(xqValue instanceof XQFunction) {
			// Its a function. Let Do something special
			out = new JsCallback((XQFunction)xqValue, xqCx);
		} else if(xqValue.isEmpty()) {
			out = Undefined.instance;
		} else {
			out = xqValue.toJava();
			out = Context.javaToJS(out, scope);
		}
		
		return out;
		} finally {
			Context.exit();
		}
	}
	
	private static Object jsMap(Map xqValue, QueryContext xqCx) throws QueryException {
		java.util.Map<String, Object> out = new HashMap<String, Object>();
		java.util.Map<String, Function> functions = new HashMap<String, Function>();
		for(Item key : xqValue.keys()) {
			Value item = xqValue.get(key, null);
			if(item instanceof Map) {
				out.put((String)key.toJava(), jsMap((Map)item, xqCx));
			} else if(key != selfPropName && item instanceof XQFunction) { 
			    functions.put((String)key.toJava(), (Function) jsValue(item, xqCx));
			} else {
				out.put((String)key.toJava(), jsValue(item, xqCx));	
			}
		}
		
		out.put("_x_q__callbacks__", functions);
		return out;
	}
	
	private static Object jsArray(Array xqValue, QueryContext xqCx) throws QueryException {
		Context cx = Context.enter();
	    try {
		    long size = xqValue.arraySize();
	    	Object[] out = new Object[(int) size];
		    for(int i = 0; i < size; i++) {
		    	out[i] = jsValue(xqValue.get((long)i), xqCx);
		    }
	    
	    	return cx.newArray(scope, out);
	    } finally {
	    	Context.exit();
	    }
	}

	public static Object[] jsValues(Value[] args, QueryContext xqCx) throws QueryException {
		List<Object> out = new ArrayList<Object>(args.length);
		for(Value v : args) {
			Object jsV = jsValue(v, xqCx);
			if(jsV != Undefined.instance) {
				if(!(jsV instanceof Object[]) || ((Object[])jsV).length > 0) {
					out.add(jsV);
				}
			}
		}
		return out.toArray(new Object[0]);
	}

	public static Value[] xqValues(Object[] args, QueryContext xqCx) throws QueryException {
		Context cx = Context.enter();
		try {
			Value[] values = new Value[args.length];
			for(int i = 0; i < args.length; i++ ) {
				values[i] = Js.xqValue(args[i], cx, scope, null, xqCx);
			}
			return values;
		} finally {
			Context.exit();
		}
	}
	
	public Str pretty(Item item) throws QueryException {
		Context cx = Context.enter();
		try {
			if(item instanceof JsObject) {
				return Js.pretty(cx, ((JsObject)item).getScope());
			} 
			
			Object target = Js.jsValue(item, queryContext);
			return target instanceof Scriptable ? 
					Js.pretty(cx, (Scriptable)target) : Str.get((String)target.toString());
		} finally {
			Context.exit();
		}
	}

	public static Str pretty(Context cx, Scriptable scopeIn) {
		Str out = Str.get((String)NativeJSON.stringify(cx, scope, scopeIn, null, null));
		return out;
	}
}
