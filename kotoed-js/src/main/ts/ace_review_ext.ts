import * as ace from "brace";
import 'ts/types_ex/ace/define'

// TODO remove this file

ace.define("ace/ext/review", [
    "require",
    "exports",
    "module",
    "ace/edit_session",
    "ace/layer/text",
    "ace/config",
    "ace/lib/dom"],
    function(_acequire, _exports, _module) {
        "use strict";
        console.log("Hello from ace ext!");
    }
);


(function() {
    ace.acequire("ace/ext/review");
})();