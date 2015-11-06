# xq-rhino

Javascript CommonJs implementation for XQuery 3.1 leveraging the Rhino Javascript library.

## Why?

Use javascript in XQuery scripts and services or test new javascript npm module with XQuery! Additionally, with the 
prevlelance of node.js in resent years. Algorithms and libraries exist in the `node` space that would be a
shame to not use in your next XQuery project.

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
This module currently require [BaseX][0]

### Contribute
If you like what you see, or have any ideas to make it better, feel free to leave feedback, make a pull request, log an issue or simply question ask a question! 

## Usage 
This module is not intended to help write better javascript, but leverage it. The most common use case is loading
existing modules and using their functionality in your XQuery and RESTXQ solutions!

For example the following modules have been tested:
* moment.js
* remarkable.js
* mustache.js
* browserfy.js

Let me know if any other module works for you!

### Interop

The module, `xq-rhino` provides a simple mapping between the Rhino `Java` javascript engine and BaseX. 
It leverages the new `map` and `array` data types introduced in XQuery 3.1.

In order to provide a seamless scripting experience, all script objects are automatically
mapped to XQuery objects. This allows for the use of the `?` operator when querying objects or arrays:

```xquery
let $obj := js:new('{
  first: "John", 
  last: "Doe", 
  name: function () { first + ' ' + last;}
')
return
  'His name is ' || $obj?name() || '.'
```

Additionlly, script objects and XQuery objects are interchangable, including
function items. For example, its possible to pass XQuery functions into javascript functions and vice versa:

```xquery
let $jsFunc := js:new('function (callback) { 
  callback("Hey XQuery!"); 
}')
return
  $jsFunc(trace(?))
```

This goes the same for maps and arrays.

### Methods
The module provides a few helper methods for interacting with javascript objects. 

#### new
New allows for contstruction and execution of any arbitray javascript code. For example,
to create an array and then query it with the new XQuery `?` operator you could do the 
following:

```xquery
let $array := js:new('[1,2,3,4,5,6,7,8]')
return
  $array?2
```

#### require
The require method implements the CommonJS pattern as used with Node.js. 

For example, if we wanted to load `mustache` with the node modules stored at `/home/user/modules` we would do the following: 

```xquery
let $require := js:require('/home/user/modules/')
let $mustache := $require('Mustache')
return
  $mustache?render('Hello {{name}}!', map { 'name': 'world' })
```

#### keys
The `keys` method returns all the keys found in a script object. Operates exactly like `map:keys`. For example, the 
following would return: `first` `last`

```xquery
let $obj := js:new('{ first: "John", last: "Doe" }')
return
  js:keys($obj)
```

#### get

Get allows an alternative means for the retrieval of script object properties.  

```xquery
let $obj := js:new('{name: "world"}')
return
  js:get($obj, 'name')
```

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

### Other Examples

#### Moment.js
Here is a simple example of loading and using [Moment.js][4].

```xquery
  let $require := js:require('path/to/modules/dir/')
  let $moment := $require('Moment')
  return
   $moment()?add('-1','day')?fromNow(())
```

#### Remarkable.js

And another with [Remarkable][5]:

Here is the example in the remarkable page:
```javascript
var Remarkable = require('remarkable');
var md = new Remarkable();

console.log(md.render('# Remarkable rulezz!'));
```

And the equivalent with `xq-rhino`, including the defining of the modules directory.

```xquery
  let $require := js:require('path/to/modules/dir/')
  let $rm := $require('Remarkable')
  let $md := js:new($rm)
  return 
    $md?render('# Remarkable rulezz! ', ()) => trace()
```

## Unit Tests
Clone the repo and run ``basex -t`` within the repo's directory to run the unit tests.

## Shout Out!
If you like what you see here please star the repo and follow me on [github][1] or [linkedIn][2]

[0]: http://www.basex.org
[1]: https://github.com/james-jw/xqpm
[2]: https://www.linkedin.com/pub/james-wright/61/25a/101
[3]: https://github.com/james-jw/xqpm
[4]: http://momentjs.com/
[5]: https://github.com/jonschlinkert/remarkable
