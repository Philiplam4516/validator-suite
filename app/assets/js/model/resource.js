define(["model/model", "model/assertions", "util/Util"], function (Model, Assertions, Util) {

    "use strict";

    var Resource;

    Resource = Model.extend({

        defaults: {
            url: "",
            lastValidated: null,
            warnings: 0,
            errors: 0
        },

        assertions: new Assertions(),

        init: function () {
            //this.id = this.get("resourceUrl");
        },

        url: function () { return "../resources/?resource=" + this.get("url"); },

        reportUrl: function () { return "../assertions/?resource=" + encodeURIComponent(this.get("url")); },

        search: function (search) {
            return this.get("url").toLowerCase().indexOf(search.toLowerCase()) > -1;
        }

        /*validate: function (attrs) {
            if (!attrs.resourceUrl || attrs.resourceUrl.length < 1) {
                logger.warn("Resource url required");
                return "Resource url required";
            }
        },*/

    });

    Resource.View = Resource.View.extend({

        templateId: "resource-template",

        templateOptions: function () {
            return {
                reportUrl : this.model.reportUrl()
            };
        },

        addSearchHandler: function () {
            var collec = this.options.assertions, input;
            if (!collec) { return; }
            this.$(".actions input[name=search]").bind("keyup change", function () {
                input = this;
                setTimeout(function () {
                    collec.view.search(input.value, input);
                }, 0);
            });
        },

        afterRender: function () {
            this.addSearchHandler();
        }

    });

    Resource.fromHtml = function ($article) {
        var value = Util.valueFrom($article);
        return {
            id: $article.attr("data-id"),
            url: value('data-url'),
            lastValidated: {
                timestamp: value('data-lastValidated'),
                legend1: value('data-lastValidated-legend1'),
                legend2: value('data-lastValidated-legend2')
            },
            warnings: parseInt(value('data-warnings'), 10),
            errors: parseInt(value('data-errors'), 10)
        };
    };

    return Resource;

});