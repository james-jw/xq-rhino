# xq-rhino

Javascript and CommonJs implementation for XQuery 3.1. This version of the library leverages the Rhino Javascript library.

## Why?

Leverage javascript in your XQuery scripts and services or test your new javascript npm module with XQuery! Additionally with the 
prevlelance of node.js in resent years. Many algorithms and libraries exist in the `node` space that would be
ashame to not use in your next XQuery project.

## Installation
Copy the ``xq-rhino-x.jar`` into your ``basex\lib`` directory 

Or use [xqpm][3] to do it for you:
```
xqpm xq-rhino
```

### Declaration
To use the module in your scripts, import it:

```xquery
import module namespace js = 'http://xq-rhino';
```

### Version 0.1-BETA
This module is currently in Beta and should be used with caution. Especially in scenarios involving the
writing of sensitive data. 

### Dependencies

## How 
This module is not intended to help write better javascript, but leverage it. The most common use case will be to load
and existing module, or set of modules and use their functionality in your solutions!

For example the following modules have been tested:
* moment.js
* remarkable.js
* mustache.js
* browserfy.js

The `xq-rhino` module provides a simple mapping between the rhino javascript engine and BaseX. 
It leverages the new `map` and `array` data types introduced in XQuery 3.1.

### Interop
In order to provide a seamless scripting experience, all script objects are automatically
mapped to XQuery objects when mapped to an XQuery variable. This allows for the use of
the `?` operator for example:

```xquery
let $obj := js:new('{first: "John", last: "Doe", name: function () { first + ' ' + last;}')
return
  'His name is ' || $obj?name() || '.'
```

Additionlly, script objects and XQuery objects are interchangable, including
function items. For example, you may pass XQuery functions into script functions and vice versa.

```xquery
let $jsFunc := js:new('function (callback) { callback("Hey XQuery!"); }')
return
  $jsFunc(trace(?))
```

This goes the same for maps and arrays.

### Methods
The module provides a few helper methods for interacting with javascript objects. The 
most powerful is `new`.

#### new
New allows for contstruction and execution of any arbitray javascript code. For example
to create an array and then query it with the new XQuery `?` operator you could do the 
following:

```xquery
let $array := js:new('[1,2,3,4,5,6,7,8]')
return
  $array?2
```

#### require
The require implements the common js pattern as used in node.js. 

For example, if you wanted to load the mustache and your node modules 
are stored at `/home/user/modules` you would do the following: 

```xquery
let $require := js:require('/home/user/modules/')
let $mustache := $require('Mustache')
return
  $mustache?render('Hello {{name}}!', map { 'name': 'world' })
```

#### attach
Attach, unlike require simply evaluates and attaches a script to the current script context. This 
is milar to how a browser works and thus conflicts can arise.

#### keys
Returns all the keys in the script objects. Exactly how `map:keys` works. For example, the 
following would return: `first` `last`

```xquery
let $obj := js:new('{ first: "John", last: "Doe" }')
return
  js:keys($obj)
```

#### get

Get allows for the retrieval of script properties.  

```xquery
let $obj := js:new('{name: "world"}')
return
  js:get($obj, 'name')
```

Generally this method is not needed since the XQuery `?` can be used to accomplish the same:

```xquery
let $obj := js:new('{name: "world"}')
return
  $obj?name
```

#### put

As the name eludes. Allows for putting a value. For example:

```xquery
let $obj := js:new('{}')
  => js:put('first', 'John')
  => js:put('last', 'Doe')
return
  $obj?first
```

#### remove
Removes a key from a script object:

```xquery
let $obj := js:new('{first:"John"}')
let $empty := js:remove($obj, 'first')
return 
  $empty
```

### Module Example

#### Moment.js
```xquery
  let $require := js:require('/modules/dir/')
  let $moment := $require('Moment')
  return
   $moment()?add('-1','day')?fromNow(())
```

#### Remarkable.js

```xquery
  let $require := js:require(file:temp-dir())
  let $rm := $require('Remarkable')
  let $remarkable := js:new($rm)
  return 
    $remarkable?render('# Remarkable rulezz! ', ())
```

## Unit Tests
Clone the repo and run ``basex -t`` within the repo's directory to run the unit tests.

## Shout Out!
If you like what you see here please star the repo and follow me on [github][1] or [linkedIn][2]

[0]: http://www.basex.org
[1]: https://github.com/james-jw/xqpm
[2]: https://www.linkedin.com/pub/james-wright/61/25a/101
[3]: https://github.com/james-jw/xqpm
