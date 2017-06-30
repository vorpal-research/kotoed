/**
 * Created by gagarski on 6/27/17.
 */

import $ = require('jquery');

require("bootstrap-less/js/bootstrap.js");
require("less/hello.less");

$(document).ready(function() {
    console.log("READY!!!");
    $("#select-me").html("Hello!");
});
