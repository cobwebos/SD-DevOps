Behaviour.specify("PRE.console-output.tail", 'progressiveTextTail', 0, function (e) {
    var lines = parseInt(e.getAttribute("lines"));   // # of lines to display
    var href = e.getAttribute("href");    //
    var offset = Math.max(0,parseInt(e.getAttribute("offset")));  // byte offset of the log file to fetch next
    var consoleAnnotator = undefined;

    function fetchNext() {
        var headers = {};
        if (consoleAnnotator != undefined)
            headers["X-ConsoleAnnotator"] = consoleAnnotator;

        new Ajax.Request(href, {
            method: "post",
            parameters: {"start": offset},
            requestHeaders: headers,
            onComplete: function (rsp, _) {
                var text = rsp.responseText;
                if (text != "") {// there was some change
                    text = e.innerHTML + text;

                    // how many lines do we have ?
                    var curLines = text.split('\n');

                    // keep only the last N lines
                    if (curLines.length>lines) {
                        curLines = curLines.slice(curLines.length-lines);
                        text = curLines.join('\n');
                    }

                    // TODO: test with IE
                    e.innerHTML = text;

                    // not applying behaviour as it ends up reapplying on the same text multiple times
                    // Behaviour.applySubtree(p);
                }

                offset = rsp.getResponseHeader("X-Text-Size");
                consoleAnnotator = rsp.getResponseHeader("X-ConsoleAnnotator");
                if (rsp.getResponseHeader("X-More-Data") == "true") {
                    setTimeout(function () {
                        fetchNext();
                    }, 1000);
                } else {
                    e.addClassName("done")
                }
            }
        });
    }

    fetchNext();
});