(:
 : @author James Wright
 : Tests for xq-rhino library
 :)
module namespace test = 'http://basex.org/modules/xqunit-tests';
import module namespace js = 'http://xq-rhino' at '../src/xq-rhino.xqm';
declare %unit:ignore variable $test:mustache := 'https://raw.githubusercontent.com/janl/mustache.js/master/mustache.js';
declare %unit:ignore variable $test:moment := 'http://momentjs.com/downloads/moment.js';
declare %unit:ignore variable $test:remarkable := 'https://raw.githubusercontent.com/jonschlinkert/remarkable/dev/dist/remarkable.min.js';

declare %unit:ignore variable $test:retrieve := function($uri, $binary) {
  let $req := <http:request method="GET" />
  let $raw := http:send-request($req, $uri)[2] 
  let $code := if(not($binary)) then $raw
               else convert:binary-to-string($raw)
  return 
    $code
};

(: Create all the npm modules for testing :)
declare %unit:ignore %unit:before function test:before() {
  (
    file:write-text(file:temp-dir() || 'mustache.js', $test:retrieve($test:mustache, false())),
    file:write-text(file:temp-dir() || 'moment.js', $test:retrieve($test:moment, true())),
    file:write-text(file:temp-dir() || 'remarkable.js', $test:retrieve($test:remarkable, false()))
  )
};

declare %unit:ignore %unit:test function test:pass-map-to-method() {
  let $code := http:send-request(<http:request method="GET" />, $test:mustache)[2]
  let $mustache := js:attach('Mustache', $code)
  let $render := $mustache?render
  let $result := $render('Hello {{name}}!', map { 'name' : 'world' }, ())
  return
   unit:assert-equals($result, 'Hello world!')
};

declare %unit:ignore %unit:test function test:pass-func-to-method() {
  let $code := http:send-request(<http:request method="GET" />, $test:mustache)[2]
  let $mustache := js:attach('Mustache', $code)
  let $result := $mustache?render('Hello {{name}}!', map { 
    'name' : function () { 
       'world'
  }
  }, ())
  return
   unit:assert-equals($result, 'Hello world!')
};

declare %unit:ignore %unit:test function test:dot-notation() {
  let $script := js:new('{ name: "world" }')
  return
    (unit:assert-equals($script?name, 'world'))
};

declare %unit:ignore %unit:test function test:xq-to-js-array() {
  let $script := js:new('[true,2,3]')
  return 
     unit:assert-equals($script?2, 2)
};

declare %unit:ignore %unit:test function test:js-xq-bool-true() {
  let $script := js:new('[true,2,3]')
  return 
     unit:assert-equals($script?1, true())
};

declare %unit:ignore %unit:test function test:js-xq-bool-false() {
  let $script := js:new('false')
  return 
     unit:assert-equals($script, false())
};

declare %unit:ignore %unit:test function test:js-xq-bool-double() {
  let $script := js:new('200.233406')
  return 
     unit:assert-equals($script, 200.233406)
};

declare %unit:ignore %unit:test function test:js-keys() {
  let $map := js:new("{ 
    greeting: 'Hello',
    name: 'world' 
  }")
  return 
    unit:assert-equals(map:keys($map) = 'name', true())
};

declare %unit:ignore %unit:test function test:js-get() {
  let $map := js:new("{ 
    greeting: 'Hello',
    name: 'world' 
  }")
  return 
    unit:assert-equals(map:get($map, 'name'), 'world')
};

declare %unit:ignore %unit:test function test:js-get-array() {
  let $script := js:new('[true,2,3]')
  return 
     unit:assert-equals($script?1, true())
};

declare %unit:ignore %unit:test function test:js-remove() {
  let $map := js:new("{ 
    greeting: 'Hello',
    name: 'world' 
  }")
  let $after := js:remove($map, 'name')
  return 
     (unit:assert-equals(map:keys($after) = 'name', false()),
      unit:assert-equals(map:keys($after) = 'greeting', true())
     )
};

declare %unit:ignore %unit:test function test:js-put() {
  let $map := js:new("{ 
    greeting: 'Hello'
  }")
  return 
    (js:put($map, 'name', 'world'),
     unit:assert-equals(js:get($map, 'name'), 'world'))
};

declare %unit:ignore %unit:test function test:js-as-func() {
  let $map := js:new("{ 
    greeting: 'Hello',
    name: function () { return 'world'; }
  }")
  return 
     unit:assert-equals(js:get($map, 'name')(), 'world')
};

declare %unit:ignore %unit:test function test:to-string() {
   let $map := js:new("{ 
    greeting: 'Hello',
    name: function () { return 'world'; }
  }")
  return unit:assert-equals(js:pretty($map), '{"greeting":"Hello"}')
};

declare %unit:ignore %unit:test function test:to-xq-map() {
  let $map := js:new("{ 
    greeting: 'Hello',
    name: function () { return 'world'; }
  }")
  return 
     unit:assert-equals($map?greeting, 'Hello')
};

declare %unit:ignore %unit:test function test:to-xq-map-func() {
  let $map := js:new("{ 
    greeting: 'Hello',
    name: function () { return 'world'; }
  }")
  return 
     unit:assert-equals($map?name(), 'world')
};

declare %unit:ignore %unit:test function test:to-xq-map-func-with-aguments() {
  let $map := js:new("{ 
    greeting: 'Hello',
    name: function (name) { return name; }
  }")
  return 
     unit:assert-equals($map?name('world'), 'world')
};

declare %unit:ignore %unit:test function test:to-xq-map-and-back() {
  let $map := js:new("{ 
    greeting: 'Hello',
    name: function () { return 'world'; }
  }")
  return 
     unit:assert-equals(js:pretty($map), '{"greeting":"Hello"}')
};

declare %unit:ignore %unit:test function test:to-js-new-map() {(
  let $new := js:new('{ "name": "world" }')
  return 
    unit:assert-equals($new?name, 'world')
)};

declare %unit:ignore %unit:test function test:to-js-new-then-put() {(
  let $new := js:new('{}')
  let $new := js:put($new, 'name', 'hello') 
     => js:put('last', 'world')
     => js:put('greeting', function ($first, $name) {
       $first || ' ' || $name || '!'
     })
  return 
    unit:assert-equals($new?greeting($new?name, $new?last), 'hello world!')
)};

declare %unit:ignore %unit:test function test:to-js-new-with-xq-map() {(
  let $new := js:new(map { 'name': 'world' })
  return 
    unit:assert-equals($new?name, 'world')
)};

declare %unit:ignore %unit:test function test:to-js-new-function() {(
  let $new := js:new(function ($name) { $name || $test:mustache })
  return 
    unit:assert-equals($new('world'), 'world' || $test:mustache)
)};

declare %unit:ignore %unit:test function test:to-js-new-array() {
  let $new := js:new(array { '1', '2', '3'})
  return
    unit:assert-equals($new?1, '1')
};

declare %unit:ignore %unit:test function test:js-new-date() {
  let $new := js:new('new Date()')
  return
    unit:assert-equals(xs:dateTime($new) => minutes-from-dateTime(), current-dateTime() => minutes-from-dateTime())
};

(: Percision is lost going Too Javascript.:)
declare %unit:test function test:xq-pass-date() {
  let $dateFunc := js:new('function(date) { return date; }')
  let $time := current-dateTime()
  return
     unit:assert-equals($dateFunc($time) => minutes-from-dateTime(), $time => minutes-from-dateTime())
};

declare %unit:ignore %unit:test function test:common-js-require() {
  let $require := js:require(file:temp-dir())
  return
    unit:assert(exists($require))
};

declare %unit:ignore %unit:test function test:require-module() {
  let $require := js:require(file:temp-dir())
  let $mustache := $require('Mustache')
  return
   unit:assert(exists($mustache))
};

declare %unit:ignore %unit:test function test:moment-js() {
  let $require := js:require(file:temp-dir())
  let $moment := $require('Moment')
  return
   unit:assert-equals($moment()?add('-1','day')?fromNow(()), 'a day ago')
};

declare %unit:ignore %unit:test function test:remarkable-js() {
  let $require := js:require(file:temp-dir())
  let $rm := $require('Remarkable')
  let $rendered := js:new($rm)?render('# Remarkable rulezz! ', ())
  return 
   unit:assert-equals(data(($rendered => parse-xml())/h1), 'Remarkable rulezz!')
};

declare %unit:ignore %unit:after function test:after() {
  ( file:delete(file:temp-dir() || 'moment.js'),
    file:delete(file:temp-dir() || 'mustache.js'),
    file:delete(file:temp-dir() || 'remarkable.js')
  )
};
