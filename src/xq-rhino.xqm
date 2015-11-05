(: Bindings for rhino BaseX Query module
 : @author James Wright
 : @email james.jw hotmail com
 : @date 10/24/15 
 :)
module namespace local = 'http://xq-rhino';
import module namespace js = 'org.jw.basex.js.rhino.js';

(: This variable is used to maintain the js object 
 : when mapped to an XQuery Map
 :)
declare variable $local:self-prop := js:self-property();

(: Returns the commonJs require object for requesting 
 : dependecies
 : @param $paths List of paths where modules can be found.
 :)
declare function local:require($paths as xs:string) {
  js:require($paths, local:map#1)
};

declare function local:require($paths as xs:string, $options as map(*)) {
  js:require($paths)
};

(: Imports the code provided into the root scope.
 : @param $code Javascript evaluate against the root scope
 :
 : @example 
 : ```xquery
 :   let $script := local:import('http://www.mustache.js')
 : ```
 :)
declare function local:import($code) {
  js:attach($code)
};

(: Imports the required module and pulls the specified named
 : object from its context.
 : @param $name Name of object to pull form context after evaluation.
 : @param $code Code to evaluate.
 :)
declare function local:import($name, $code) {
   js:attach($name, $code)
};

(: Imports the code and returns it as a map or function.
 : @param $code to import
 :)
declare function local:attach($code) as map(*) {
  local:import($code)
};

(: Imports the code and returns the property matching the $name provided
 : from the imported code scope and returns it as a map or function.
 : @param $name Name to property to pull from scope as map
 : @param $code Code to import into the root scope
 :)
declare function local:attach($name, $code) as map(*) {
  local:import($code) => js:get($name)
};

(: Returns a sequence of keys from the provided javascript object 
 : @param $obj Item to retrieve the keys from
 :)
declare function local:keys($obj as function(*)) {
  map:keys($obj)
};

(: Retrieves a property from an javascript object.
 : @param $obj Item to retrieve property from.
 : @param $key Key of property to retrieve.
 :)
declare function local:get($obj, $key) {
  js:get($obj, $key)
};

(: Retrieves a property from an javascript object.
 : @param $obj Item to retrieve property from.
 : @param $key Key of property to retrieve.
 :)
declare function local:get($obj, $key, $recursive as xs:boolean) {
  js:get($obj, $key, $recursive)
};

(: Calls the javascript item as a contstructor
 : with the provided arguments 
 : @param $proto Function to call as a constructor
 :)
declare function local:new($item) {
  js:construct($item)
};

(: Calls the javascript item as a contstructor
 : with the provided arguments 
 : @param $proto Function to call as a constructor
 : @param $args Arguments to pass to the constructor
 :)
declare function local:new($proto, $args) {
  js:construct($proto, $args)
};

(: Add the key and value provided to the javascript object.
 : @param $obj Object to set property on
 : @param $key Property to set
 : @param $value Value to property too.
 :)
declare function local:put($obj, $key, $value) {
  js:put($obj, $key, $value)
};

(: Removes the keys specified from the javascript object
 : @param $obj Item to remove keys from
 : @param $keys Properties to remove
 : @return The object the key was removed from with the key removed.
 :)
declare function local:remove($obj, $keys) {
  js:remove($obj, $keys)
};

(: Returns the Javascript object as pretty JSON
 : @param $obj Object to pretty print.
 :)
declare function local:pretty($obj) as xs:string {
  js:pretty($obj)
};

(: Returns the javascript object as an XQuery map.
 : This includes functions the releavate functions
 : @param $obj Objects to return as a map
 :)
declare function local:map($obj) as map(*) {
   local:map($obj, true())  
};

(: Maps a javascript function or object to an XQuery map
 : Function items will remain and not traversed. 
 :
 : By default only the top level is mapped. Subsequent mapping 
 : will be required manually. Alternatively, the second paramter,
 : if ``true`` will recursively map the javascript object.
 :
 : @param $obj Javascript object to map 
 : @param $recursive True or false whether to map recursively through the tree. Defaults to false
 :)
declare function local:map($obj, $recursive as xs:boolean) {
  map:merge((
    for $key in js:keys($obj, $recursive) 
    let $value := js:get($obj, $key, $recursive)
    return map { $key :
      if($recursive and (js:is-map($value) or js:is-array($value))) 
      then local:map($value, $recursive)
      else $value 
    },
    map { $local:self-prop: $obj }
  ))  
};

(: Returns the javascript object as an XQuery array.
 : This includes functions the relative functions.
 : If called on a map, the values will be returned as an array.
 : use ``keys`` for retrieving an array of keys
 :
 : @param $obj Objects to return as a map
 :)
declare function local:array($obj, $recursive as xs:boolean) as array(*) {
  array {
    for $key in js:keys($obj)
    return js:get($obj, $key)
  }
};

(: Returns the type of javascript object as standard 
 : javascript type name.
 : @param $obj Item to retrieve type information from 
 :)
declare function local:type-of($obj) as xs:string {
   js:type-of($obj)
};