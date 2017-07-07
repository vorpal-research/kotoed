/**
 * Created by gagarski on 6/27/17.
 */

import * as $ from 'jquery';

require("bootstrap-less/js/bootstrap.js");
require("less/hello.less");

$(document).ready(function() {
    console.log("READY!!!");
    $("#select-me").html("Hello!");
});
