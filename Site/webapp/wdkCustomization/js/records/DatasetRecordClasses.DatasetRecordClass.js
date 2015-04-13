"use strict";

/* global _, Wdk, wdk */
/* jshint esnext: true, eqnull: true, -W014 */

/**
 * This file provides a custom Record Component which is used by the new Wdk
 * Flux architecture.
 *
 * The sibling file DatasetRecordClasses.DatasetRecordClass.js is generated
 * from this file using the jsx compiler. Eventually, this file will be
 * compiled during build time--this is a short-term solution.
 *
 * `wdk` is the legacy global object, and `Wdk` is the new global object
 */

wdk.namespace("eupathdb.records", function (ns) {
  "use strict";

  var React = Wdk.React;

  // Use Element.innerText to strip XML
  function stripXML(str) {
    var div = document.createElement("div");
    div.innerHTML = str;
    return div.textContent;
  }

  // format is {text}({link})
  var formatLink = function formatLink(link, opts) {
    opts = opts || {};
    var newWindow = !!opts.newWindow;
    var match = /(.*)\((.*)\)/.exec(link.replace(/\n/g, " "));
    if (match) {
      var text = stripXML(match[1]);
      var url = match[2];
      return React.createElement(
        "a",
        { target: newWindow ? "_blank" : "_self", href: url },
        text
      );
    }
    return null;
  };

  var renderPrimaryPublication = function renderPrimaryPublication(publication) {
    var pubmedLink = publication.find(function (pub) {
      return pub.get("name") == "pubmed_link";
    });
    return formatLink(pubmedLink.get("value"), { newWindow: true });
  };

  var renderPrimaryContact = function renderPrimaryContact(contact, institution) {
    return contact + ", " + institution;
  };

  var renderSourceVersion = function renderSourceVersion(version) {
    var name = version.find(function (v) {
      return v.get("name") === "version";
    });
    return name.get("value") + " (The data provider's version number or publication date, from" + " the site the data was acquired. In the rare case neither is available," + " the download date.)";
  };

  var Organisms = React.createClass({
    displayName: "Organisms",

    render: function render() {
      var organisms = this.props.organisms;

      if (!organisms) {
        return null;
      }return React.createElement(
        "div",
        null,
        React.createElement(
          "h2",
          null,
          "Organisms this data set is mapped to in PlasmoDB"
        ),
        React.createElement(
          "ul",
          null,
          organisms.split(/,\s*/).map(this._renderOrganism).toArray()
        )
      );
    },

    _renderOrganism: function _renderOrganism(organism, index) {
      return React.createElement(
        "li",
        { key: index },
        React.createElement(
          "i",
          null,
          organism
        )
      );
    }
  });

  var Searches = React.createClass({
    displayName: "Searches",

    render: function render() {
      var searches = this.props.searches.get("rows").filter(this._rowIsQuestion);

      if (searches.size === 0) {
        return null;
      }return React.createElement(
        "div",
        null,
        React.createElement(
          "h2",
          null,
          "Search or view this data set in PlasmoDB"
        ),
        React.createElement(
          "ul",
          null,
          searches.map(this._renderSearch).toArray()
        )
      );
    },

    _rowIsQuestion: function _rowIsQuestion(row) {
      var type = row.find(function (attr) {
        return attr.get("name") == "target_type";
      });
      return type && type.get("value") == "question";
    },

    _renderSearch: function _renderSearch(search, index) {
      var name = search.find(function (attr) {
        return attr.get("name") == "target_name";
      }).get("value");
      var question = this.props.questions.find(function (q) {
        return q.get("name") === name;
      });

      if (question == null) {
        return null;
      }var recordClass = this.props.recordClasses.find(function (r) {
        return r.get("fullName") === question.get("class");
      });
      var searchName = "Identify " + recordClass.get("displayNamePlural") + " by " + question.get("displayName");
      return React.createElement(
        "li",
        { key: index },
        React.createElement(
          "a",
          { href: "/a/showQuestion.do?questionFullName=" + name },
          searchName
        )
      );
    }
  });

  var Links = React.createClass({
    displayName: "Links",

    render: function render() {
      var links = this.props.links;

      if (links.get("rows").size === 0) {
        return null;
      }return React.createElement(
        "div",
        null,
        React.createElement(
          "h2",
          null,
          "Links"
        ),
        React.createElement(
          "ul",
          null,
          " ",
          links.get("rows").map(this._renderLink).toArray(),
          " "
        )
      );
    },

    _renderLink: function _renderLink(link, index) {
      var hyperLink = link.find(function (attr) {
        return attr.get("name") == "hyper_link";
      });
      return React.createElement(
        "li",
        { key: index },
        formatLink(hyperLink.get("value"))
      );
    }
  });

  var Contacts = React.createClass({
    displayName: "Contacts",

    render: function render() {
      var contacts = this.props.contacts;

      if (contacts.get("rows").size === 0) {
        return null;
      }return React.createElement(
        "div",
        null,
        React.createElement(
          "h4",
          null,
          "Contacts"
        ),
        React.createElement(
          "ul",
          null,
          contacts.get("rows").map(this._renderContact).toArray()
        )
      );
    },

    _renderContact: function _renderContact(contact, index) {
      var contact_name = contact.find(function (c) {
        return c.get("name") == "contact_name";
      });
      var affiliation = contact.find(function (c) {
        return c.get("name") == "affiliation";
      });
      return React.createElement(
        "li",
        { key: index },
        contact_name.get("value"),
        ", ",
        affiliation.get("value")
      );
    }
  });

  var Publications = React.createClass({
    displayName: "Publications",

    render: function render() {
      var publications = this.props.publications;

      var rows = publications.get("rows");
      if (rows.size === 0) {
        return null;
      }return React.createElement(
        "div",
        null,
        React.createElement(
          "h4",
          null,
          "Publications"
        ),
        React.createElement(
          "ul",
          null,
          rows.map(this._renderPublication).toArray()
        )
      );
    },

    _renderPublication: function _renderPublication(publication, index) {
      var pubmed_link = publication.find(function (p) {
        return p.get("name") == "pubmed_link";
      });
      return React.createElement(
        "li",
        { key: index },
        formatLink(pubmed_link.get("value"))
      );
    }
  });

  var ContactsAndPublications = React.createClass({
    displayName: "ContactsAndPublications",

    render: function render() {
      var _props = this.props;
      var contacts = _props.contacts;
      var publications = _props.publications;

      if (contacts.get("rows").size === 0 && publications.get("rows").size === 0) {
        return null;
      }return React.createElement(
        "div",
        null,
        React.createElement(
          "h2",
          null,
          "Additional Contacts and Publications"
        ),
        React.createElement(Contacts, { contacts: contacts }),
        React.createElement(Publications, { publications: publications })
      );
    }
  });

  var ReleaseHistory = React.createClass({
    displayName: "ReleaseHistory",

    render: function render() {
      var history = this.props.history;

      if (history.get("rows").size === 0) {
        return null;
      }return React.createElement(
        "div",
        null,
        React.createElement(
          "h2",
          null,
          "Data Set Release History"
        ),
        React.createElement(
          "table",
          null,
          React.createElement(
            "thead",
            null,
            React.createElement(
              "tr",
              null,
              React.createElement(
                "th",
                null,
                "EuPathDB Release"
              ),
              React.createElement(
                "th",
                null,
                "Genome Source"
              ),
              React.createElement(
                "th",
                null,
                "Annotation Source"
              ),
              React.createElement(
                "th",
                null,
                "Notes"
              )
            )
          ),
          React.createElement(
            "tbody",
            null,
            history.get("rows").map(this._renderRow).toArray()
          )
        )
      );
    },

    _renderRow: function _renderRow(attributes) {
      var attrs = _.indexBy(attributes.toJS(), "name");

      var releaseDate = attrs.release_date.value.split(/\s+/)[0];

      var release = attrs.build.value == 0 ? "Initial release" : "" + attrs.project.value + " " + attrs.release_number.value + " " + releaseDate;

      var genomeSource = attrs.genome_source.value ? attrs.genome_source.value + " (" + attrs.genome_version.value + ")" : "";

      var annotationSource = attrs.annotation_source.value ? attrs.annotation_source.value + " (" + attrs.annotation_version.value + ")" : "";

      return React.createElement(
        "tr",
        null,
        React.createElement(
          "td",
          null,
          release
        ),
        React.createElement(
          "td",
          null,
          genomeSource
        ),
        React.createElement(
          "td",
          null,
          annotationSource
        ),
        React.createElement(
          "td",
          null,
          attrs.note.value
        )
      );
    }
  });

  var Versions = React.createClass({
    displayName: "Versions",

    render: function render() {
      var versions = this.props.versions;

      var rows = versions.get("rows");

      if (rows.size === 0) {
        return null;
      }return React.createElement(
        "div",
        null,
        React.createElement(
          "h2",
          null,
          "Provider's Version"
        ),
        React.createElement(
          "p",
          null,
          "The data set version shown here is the data provider's version number or publication date indicated on the site from which we downloaded the data. In the rare case that these are not available, the version is the date that the data set was downloaded."
        ),
        React.createElement(
          "table",
          null,
          React.createElement(
            "thead",
            null,
            React.createElement(
              "tr",
              null,
              React.createElement(
                "th",
                null,
                "Organism"
              ),
              React.createElement(
                "th",
                null,
                "Provider's Version"
              )
            )
          ),
          React.createElement(
            "tbody",
            null,
            rows.map(this._renderRow).toArray()
          )
        )
      );
    },

    _renderRow: function _renderRow(attributes) {
      var attrs = _.indexBy(attributes.toJS(), "name");
      return React.createElement(
        "tr",
        null,
        React.createElement(
          "td",
          null,
          attrs.organism.value
        ),
        React.createElement(
          "td",
          null,
          attrs.version.value
        )
      );
    }
  });

  var Graphs = React.createClass({
    displayName: "Graphs",

    render: function render() {
      var graphs = this.props.graphs;

      var rows = graphs.get("rows");
      if (rows.size === 0) {
        return null;
      }return React.createElement(
        "div",
        null,
        React.createElement(
          "h2",
          null,
          "Example Graphs"
        ),
        React.createElement(
          "ul",
          null,
          rows.map(this._renderGraph).toArray()
        )
      );
    },

    _renderGraph: function _renderGraph(graph, index) {
      var g = _.indexBy(graph.toJS(), "name");

      var displayName = g.display_name.value;

      var baseUrl = "/cgi-bin/dataPlotter.pl" + "?type=" + g.module.value + "&project_id=" + g.project_id.value + "&dataset=" + g.dataset_name.value + "&template=" + (g.is_graph_custom.value === "false" ? 1 : "") + "&id=" + g.graph_ids.value;

      var imgUrl = baseUrl + "&fmt=png";
      var tableUrl = baseUrl + "&fmt=table";

      return React.createElement(
        "li",
        { key: index },
        React.createElement(
          "h3",
          null,
          displayName
        ),
        React.createElement(
          "div",
          { className: "eupathdb-DatasetRecord-GraphMeta" },
          React.createElement(
            "h3",
            null,
            "Description"
          ),
          React.createElement("p", { dangerouslySetInnerHTML: { __html: g.description.value } }),
          React.createElement(
            "h3",
            null,
            "X-axis"
          ),
          React.createElement(
            "p",
            null,
            g.x_axis.value
          ),
          React.createElement(
            "h3",
            null,
            "Y-axis"
          ),
          React.createElement(
            "p",
            null,
            g.y_axis.value
          )
        ),
        React.createElement(
          "div",
          { className: "eupathdb-DatasetRecord-GraphData" },
          React.createElement("img", { className: "eupathdb-DatasetRecord-GraphImg", src: imgUrl })
        )
      );
    }
  });

  var IsolatesList = React.createClass({
    displayName: "IsolatesList",

    render: function render() {
      var _props$isolates$toJS = this.props.isolates.toJS();

      var rows = _props$isolates$toJS.rows;

      if (rows.length === 0) {
        return null;
      }return React.createElement(
        "div",
        null,
        React.createElement(
          "h2",
          null,
          "Isolates / Samples"
        ),
        React.createElement(
          "ul",
          null,
          rows.map(this._renderRow)
        )
      );
    },

    _renderRow: function _renderRow(attributes, index) {
      var isolate_link = attributes.find(function (attr) {
        return attr.name === "isolate_link";
      });
      return React.createElement(
        "li",
        { key: index },
        formatLink(isolate_link.value)
      );
    }

  });

  var DatasetRecord = React.createClass({
    displayName: "DatasetRecord",

    render: function render() {
      var _props = this.props;
      var record = _props.record;
      var questions = _props.questions;
      var recordClasses = _props.recordClasses;

      var attributes = record.get("attributes");
      var tables = record.get("tables");
      var titleClass = "eupathdb-DatasetRecord-title";

      var id = record.get("id");
      var summary = attributes.getIn(["summary", "value"]);
      var releaseInfo = attributes.getIn(["eupath_release", "value"]);
      var primaryPublication = tables.getIn(["Publications", "rows", 0]);
      var contact = attributes.getIn(["contact", "value"]);
      var institution = attributes.getIn(["institution", "value"]);
      var version = tables.getIn(["Version", "rows", 0]);
      var organism = attributes.getIn(["organism_prefix", "value"]);
      var organisms = attributes.getIn(["organisms", "value"]);
      var References = tables.get("References");
      var HyperLinks = tables.get("HyperLinks");
      var Contacts = tables.get("Contacts");
      var Publications = tables.get("Publications");
      var description = attributes.getIn(["description", "value"]);
      var GenomeHistory = tables.get("GenomeHistory");
      var Version = tables.get("Version");
      var ExampleGraphs = tables.get("ExampleGraphs");
      var Isolates = tables.get("Isolates");

      return React.createElement(
        "div",
        { className: "eupathdb-DatasetRecord ui-helper-clearfix" },
        React.createElement("h1", { dangerouslySetInnerHTML: {
            __html: "Data Set: <span class=\"" + titleClass + "\">" + id + "</span>"
          } }),
        React.createElement(
          "div",
          { className: "eupathdb-DatasetRecord-Container ui-helper-clearfix" },
          React.createElement("hr", null),
          React.createElement(
            "table",
            { className: "eupathdb-DatasetRecord-headerTable" },
            React.createElement(
              "tbody",
              null,
              React.createElement(
                "tr",
                null,
                React.createElement(
                  "th",
                  null,
                  "Summary:"
                ),
                React.createElement("td", { dangerouslySetInnerHTML: { __html: summary } })
              ),
              organism ? React.createElement(
                "tr",
                null,
                React.createElement(
                  "th",
                  null,
                  "Organism (source or reference):"
                ),
                React.createElement("td", { dangerouslySetInnerHTML: { __html: organism } })
              ) : null,
              primaryPublication ? React.createElement(
                "tr",
                null,
                React.createElement(
                  "th",
                  null,
                  "Primary publication:"
                ),
                React.createElement(
                  "td",
                  null,
                  renderPrimaryPublication(primaryPublication)
                )
              ) : null,
              contact && institution ? React.createElement(
                "tr",
                null,
                React.createElement(
                  "th",
                  null,
                  "Primary contact:"
                ),
                React.createElement(
                  "td",
                  null,
                  renderPrimaryContact(contact, institution)
                )
              ) : null,
              version ? React.createElement(
                "tr",
                null,
                React.createElement(
                  "th",
                  null,
                  "Source version:"
                ),
                React.createElement(
                  "td",
                  null,
                  renderSourceVersion(version)
                )
              ) : null,
              releaseInfo ? React.createElement(
                "tr",
                null,
                React.createElement(
                  "th",
                  null,
                  "EuPathDB release # / date:"
                ),
                React.createElement(
                  "td",
                  null,
                  releaseInfo
                )
              ) : null
            )
          ),
          React.createElement("hr", null),
          React.createElement(
            "div",
            { className: "eupathdb-DatasetRecord-Main" },
            React.createElement(
              "h2",
              null,
              "Detailed Description"
            ),
            React.createElement("div", { dangerouslySetInnerHTML: { __html: description } }),
            React.createElement(ContactsAndPublications, { contacts: Contacts, publications: Publications })
          ),
          React.createElement(
            "div",
            { className: "eupathdb-DatasetRecord-Sidebar" },
            React.createElement(Organisms, { organisms: organisms }),
            React.createElement(Searches, { searches: References, questions: questions, recordClasses: recordClasses }),
            React.createElement(IsolatesList, { isolates: Isolates }),
            React.createElement(Links, { links: HyperLinks }),
            React.createElement(ReleaseHistory, { history: GenomeHistory }),
            React.createElement(Versions, { versions: Version })
          )
        ),
        React.createElement(Graphs, { graphs: ExampleGraphs })
      );
    }
  });

  var Tooltip = React.createClass({
    displayName: "Tooltip",

    componentDidMount: function componentDidMount() {
      this._setupTooltip();
    },
    componentDidUpdate: function componentDidUpdate() {
      this._destroyTooltip();
      this._setupTooltip();
    },
    componentWillUnmount: function componentWillUnmount() {
      this._destroyTooltip();
    },
    _setupTooltip: function _setupTooltip() {
      if (this.props.text == null) {
        return;
      }var text = "<div style=\"max-height: 200px; overflow-y: auto; padding: 2px;\">" + this.props.text + "</div>";
      var width = this.props.width;

      this.$target = $(this.getDOMNode()).find(".wdk-RecordTable-recordLink").wdkTooltip({
        overwrite: true,
        content: { text: text },
        show: { delay: 1000 },
        position: { my: "top left", at: "bottom left", adjust: { y: 12 } }
      });
    },
    _destroyTooltip: function _destroyTooltip() {
      // if _setupTooltip doesn't do anything, this is a noop
      if (this.$target) {
        this.$target.qtip("destroy", true);
      }
    },
    render: function render() {
      // FIXME - Figure out why we lose the fixed-data-table className
      // Losing the fixed-data-table className for some reason... adding it back.
      var child = React.Children.only(this.props.children);
      child.props.className += " public_fixedDataTableCell_cellContent";
      return child;
      //return this.props.children;
    }
  });

  function datasetCellRenderer(attribute, attributeName, attributes, index, columnData, width, defaultRenderer) {
    var reactElement = defaultRenderer(attribute, attributeName, attributes, index, columnData, width);

    if (attribute.get("name") === "primary_key") {
      return React.createElement(
        Tooltip,
        {
          text: attributes.get("description").get("value"),
          width: width
        },
        reactElement
      );
    } else {
      return reactElement;
    }
  }

  ns.DatasetRecord = DatasetRecord;
  ns.datasetCellRenderer = datasetCellRenderer;
});

//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIkRhdGFzZXRSZWNvcmRDbGFzc2VzLkRhdGFzZXRSZWNvcmRDbGFzcy5qc3giXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7Ozs7Ozs7Ozs7Ozs7OztBQWNBLEdBQUcsQ0FBQyxTQUFTLENBQUMsa0JBQWtCLEVBQUUsVUFBUyxFQUFFLEVBQUU7QUFDN0MsY0FBWSxDQUFDOztBQUViLE1BQUksS0FBSyxHQUFHLEdBQUcsQ0FBQyxLQUFLLENBQUM7OztBQUd0QixXQUFTLFFBQVEsQ0FBQyxHQUFHLEVBQUU7QUFDckIsUUFBSSxHQUFHLEdBQUcsUUFBUSxDQUFDLGFBQWEsQ0FBQyxLQUFLLENBQUMsQ0FBQztBQUN4QyxPQUFHLENBQUMsU0FBUyxHQUFHLEdBQUcsQ0FBQztBQUNwQixXQUFPLEdBQUcsQ0FBQyxXQUFXLENBQUM7R0FDeEI7OztBQUdELE1BQUksVUFBVSxHQUFHLFNBQVMsVUFBVSxDQUFDLElBQUksRUFBRSxJQUFJLEVBQUU7QUFDL0MsUUFBSSxHQUFHLElBQUksSUFBSSxFQUFFLENBQUM7QUFDbEIsUUFBSSxTQUFTLEdBQUcsQ0FBQyxDQUFDLElBQUksQ0FBQyxTQUFTLENBQUM7QUFDakMsUUFBSSxLQUFLLEdBQUcsY0FBYyxDQUFDLElBQUksQ0FBQyxJQUFJLENBQUMsT0FBTyxDQUFDLEtBQUssRUFBRSxHQUFHLENBQUMsQ0FBQyxDQUFDO0FBQzFELFFBQUksS0FBSyxFQUFFO0FBQ1QsVUFBSSxJQUFJLEdBQUcsUUFBUSxDQUFDLEtBQUssQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzlCLFVBQUksR0FBRyxHQUFHLEtBQUssQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNuQixhQUFTOztVQUFHLE1BQU0sRUFBRSxTQUFTLEdBQUcsUUFBUSxHQUFHLE9BQU8sQUFBQyxFQUFDLElBQUksRUFBRSxHQUFHLEFBQUM7UUFBRSxJQUFJO09BQUssQ0FBRztLQUM3RTtBQUNELFdBQU8sSUFBSSxDQUFDO0dBQ2IsQ0FBQzs7QUFFRixNQUFJLHdCQUF3QixH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RSxlQUFlLEVBQUU7QUFDNUcsUUFBSSxZQUFZLEdBQUcsZUFBZSxDQUFDLFNBQVMsRUFBRSxhQUFhLEVBQUUsVUFBVSxFQUFFLEtBQUssRUFBRSxVQUFVLEVBQUUsS0FBSyxDQUFDLENBQUM7O0FBRW5HLFFBQUksU0FBUyxDQUFDLEdBQUcsQ0FBQyxNQUFNLENBQUMsS0FBSyxhQUFhLEVBQUU7QUFDM0MsYUFDRTtBQUFDLGVBQU87O0FBQ04sY0FBSSxFQUFFLFVBQVUsQ0FBQyxHQUFHLENBQUMsYUFBYSxDQUFDLENBQUMsR0FBRyxDQUFDLE9BQU8sQ0FBQyxBQUFDO0FBQ2pELGVBQUssRUFBRSxLQUFLLEFBQUM7O1FBQ2IsWUFBWTtPQUFXLENBQ3pCO0tBQ0gsTUFDSTtBQUNILGFBQU8sWUFBWSxDQUFDO0tBQ3JCO0dBQ0Y7O0FBRUQsSUFBRSxDQUFDLGFBQWEsR0FBRyxhQUFhLENBQUM7QUFDakMsSUFBRSxDQUFDLG1CQUFtQixHQUFHLG1CQUFtQixDQUFDO0NBQzlDLENBQUMsQ0FBQyIsImZpbGUiOiJEYXRhc2V0UmVjb3JkQ2xhc3Nlcy5EYXRhc2V0UmVjb3JkQ2xhc3MuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvKiBnbG9iYWwgXywgV2RrLCB3ZGsgKi9cbi8qIGpzaGludCBlc25leHQ6IHRydWUsIGVxbnVsbDogdHJ1ZSwgLVcwMTQgKi9cblxuLyoqXG4gKiBUaGlzIGZpbGUgcHJvdmlkZXMgYSBjdXN0b20gUmVjb3JkIENvbXBvbmVudCB3aGljaCBpcyB1c2VkIGJ5IHRoZSBuZXcgV2RrXG4gKiBGbHV4IGFyY2hpdGVjdHVyZS5cbiAqXG4gKiBUaGUgc2libGluZyBmaWxlIERhdGFzZXRSZWNvcmRDbGFzc2VzLkRhdGFzZXRSZWNvcmRDbGFzcy5qcyBpcyBnZW5lcmF0ZWRcbiAqIGZyb20gdGhpcyBmaWxlIHVzaW5nIHRoZSBqc3ggY29tcGlsZXIuIEV2ZW50dWFsbHksIHRoaXMgZmlsZSB3aWxsIGJlXG4gKiBjb21waWxlZCBkdXJpbmcgYnVpbGQgdGltZS0tdGhpcyBpcyBhIHNob3J0LXRlcm0gc29sdXRpb24uXG4gKlxuICogYHdka2AgaXMgdGhlIGxlZ2FjeSBnbG9iYWwgb2JqZWN0LCBhbmQgYFdka2AgaXMgdGhlIG5ldyBnbG9iYWwgb2JqZWN0XG4gKi9cblxud2RrLm5hbWVzcGFjZSgnZXVwYXRoZGIucmVjb3JkcycsIGZ1bmN0aW9uKG5zKSB7XG4gIFwidXNlIHN0cmljdFwiO1xuXG4gIHZhciBSZWFjdCA9IFdkay5SZWFjdDtcblxuICAvLyBVc2UgRWxlbWVudC5pbm5lclRleHQgdG8gc3RyaXAgWE1MXG4gIGZ1bmN0aW9uIHN0cmlwWE1MKHN0cikge1xuICAgIHZhciBkaXYgPSBkb2N1bWVudC5jcmVhdGVFbGVtZW50KCdkaXYnKTtcbiAgICBkaXYuaW5uZXJIVE1MID0gc3RyO1xuICAgIHJldHVybiBkaXYudGV4dENvbnRlbnQ7XG4gIH1cblxuICAvLyBmb3JtYXQgaXMge3RleHR9KHtsaW5rfSlcbiAgdmFyIGZvcm1hdExpbmsgPSBmdW5jdGlvbiBmb3JtYXRMaW5rKGxpbmssIG9wdHMpIHtcbiAgICBvcHRzID0gb3B0cyB8fCB7fTtcbiAgICB2YXIgbmV3V2luZG93ID0gISFvcHRzLm5ld1dpbmRvdztcbiAgICB2YXIgbWF0Y2ggPSAvKC4qKVxcKCguKilcXCkvLmV4ZWMobGluay5yZXBsYWNlKC9cXG4vZywgJyAnKSk7XG4gICAgaWYgKG1hdGNoKSB7XG4gICAgICB2YXIgdGV4dCA9IHN0cmlwWE1MKG1hdGNoWzFdKTtcbiAgICAgIHZhciB1cmwgPSBtYXRjaFsyXTtcbiAgICAgIHJldHVybiAoIDxhIHRhcmdldD17bmV3V2luZG93ID8gJ19ibGFuaycgOiAnX3NlbGYnfSBocmVmPXt1cmx9Pnt0ZXh0fTwvYT4gKTtcbiAgICB9XG4gICAgcmV0dXJuIG51bGw7XG4gIH07XG5cbiAgdmFyIHJlbmRlclByaW1hcnlQdWJsaWNhdGlvbiA9IGZ1bmN0aW9uIHJlbmRlclByaW1hcnlQdWJsaWNhdGlvbihwdWJsaWNhdGlvbikge1xuICAgIHZhciBwdWJtZWRMaW5rID0gcHVibGljYXRpb24uZmluZChmdW5jdGlvbihwdWIpIHtcbiAgICAgIHJldHVybiBwdWIuZ2V0KCduYW1lJykgPT0gJ3B1Ym1lZF9saW5rJztcbiAgICB9KTtcbiAgICByZXR1cm4gZm9ybWF0TGluayhwdWJtZWRMaW5rLmdldCgndmFsdWUnKSwgeyBuZXdXaW5kb3c6IHRydWUgfSk7XG4gIH07XG5cbiAgdmFyIHJlbmRlclByaW1hcnlDb250YWN0ID0gZnVuY3Rpb24gcmVuZGVyUHJpbWFyeUNvbnRhY3QoY29udGFjdCwgaW5zdGl0dXRpb24pIHtcbiAgICByZXR1cm4gY29udGFjdCArICcsICcgKyBpbnN0aXR1dGlvbjtcbiAgfTtcblxuICB2YXIgcmVuZGVyU291cmNlVmVyc2lvbiA9IGZ1bmN0aW9uKHZlcnNpb24pIHtcbiAgICB2YXIgbmFtZSA9IHZlcnNpb24uZmluZCh2ID0+IHYuZ2V0KCduYW1lJykgPT09ICd2ZXJzaW9uJyk7XG4gICAgcmV0dXJuIChcbiAgICAgIG5hbWUuZ2V0KCd2YWx1ZScpICsgJyAoVGhlIGRhdGEgcHJvdmlkZXJcXCdzIHZlcnNpb24gbnVtYmVyIG9yIHB1YmxpY2F0aW9uIGRhdGUsIGZyb20nICtcbiAgICAgICcgdGhlIHNpdGUgdGhlIGRhdGEgd2FzIGFjcXVpcmVkLiBJbiB0aGUgcmFyZSBjYXNlIG5laXRoZXIgaXMgYXZhaWxhYmxlLCcgK1xuICAgICAgJyB0aGUgZG93bmxvYWQgZGF0ZS4pJ1xuICAgICk7XG4gIH07XG5cbiAgdmFyIE9yZ2FuaXNtcyA9IFJlYWN0LmNyZWF0ZUNsYXNzKHtcbiAgICByZW5kZXIoKSB7XG4gICAgICB2YXIgeyBvcmdhbmlzbXMgfSA9IHRoaXMucHJvcHM7XG4gICAgICBpZiAoIW9yZ2FuaXNtcykgcmV0dXJuIG51bGw7XG4gICAgICByZXR1cm4gKFxuICAgICAgICA8ZGl2PlxuICAgICAgICAgIDxoMj5PcmdhbmlzbXMgdGhpcyBkYXRhIHNldCBpcyBtYXBwZWQgdG8gaW4gUGxhc21vREI8L2gyPlxuICAgICAgICAgIDx1bD57b3JnYW5pc21zLnNwbGl0KC8sXFxzKi8pLm1hcCh0aGlzLl9yZW5kZXJPcmdhbmlzbSkudG9BcnJheSgpfTwvdWw+XG4gICAgICAgIDwvZGl2PlxuICAgICAgKTtcbiAgICB9LFxuXG4gICAgX3JlbmRlck9yZ2FuaXNtKG9yZ2FuaXNtLCBpbmRleCkge1xuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGxpIGtleT17aW5kZXh9PjxpPntvcmdhbmlzbX08L2k+PC9saT5cbiAgICAgICk7XG4gICAgfVxuICB9KTtcblxuICB2YXIgU2VhcmNoZXMgPSBSZWFjdC5jcmVhdGVDbGFzcyh7XG4gICAgcmVuZGVyKCkge1xuICAgICAgdmFyIHNlYXJjaGVzID0gdGhpcy5wcm9wcy5zZWFyY2hlcy5nZXQoJ3Jvd3MnKS5maWx0ZXIodGhpcy5fcm93SXNRdWVzdGlvbik7XG5cbiAgICAgIGlmIChzZWFyY2hlcy5zaXplID09PSAwKSByZXR1cm4gbnVsbDtcblxuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGRpdj5cbiAgICAgICAgICA8aDI+U2VhcmNoIG9yIHZpZXcgdGhpcyBkYXRhIHNldCBpbiBQbGFzbW9EQjwvaDI+XG4gICAgICAgICAgPHVsPlxuICAgICAgICAgICAge3NlYXJjaGVzLm1hcCh0aGlzLl9yZW5kZXJTZWFyY2gpLnRvQXJyYXkoKX1cbiAgICAgICAgICA8L3VsPlxuICAgICAgICA8L2Rpdj5cbiAgICAgICk7XG4gICAgfSxcblxuICAgIF9yb3dJc1F1ZXN0aW9uKHJvdykge1xuICAgICAgdmFyIHR5cGUgPSByb3cuZmluZChhdHRyID0+IGF0dHIuZ2V0KCduYW1lJykgPT0gJ3RhcmdldF90eXBlJyk7XG4gICAgICByZXR1cm4gdHlwZSAmJiB0eXBlLmdldCgndmFsdWUnKSA9PSAncXVlc3Rpb24nO1xuICAgIH0sXG5cbiAgICBfcmVuZGVyU2VhcmNoKHNlYXJjaCwgaW5kZXgpIHtcbiAgICAgIHZhciBuYW1lID0gc2VhcmNoLmZpbmQoYXR0ciA9PiBhdHRyLmdldCgnbmFtZScpID09ICd0YXJnZXRfbmFtZScpLmdldCgndmFsdWUnKTtcbiAgICAgIHZhciBxdWVzdGlvbiA9IHRoaXMucHJvcHMucXVlc3Rpb25zLmZpbmQocSA9PiBxLmdldCgnbmFtZScpID09PSBuYW1lKTtcblxuICAgICAgaWYgKHF1ZXN0aW9uID09IG51bGwpIHJldHVybiBudWxsO1xuXG4gICAgICB2YXIgcmVjb3JkQ2xhc3MgPSB0aGlzLnByb3BzLnJlY29yZENsYXNzZXMuZmluZChyID0+IHIuZ2V0KCdmdWxsTmFtZScpID09PSBxdWVzdGlvbi5nZXQoJ2NsYXNzJykpO1xuICAgICAgdmFyIHNlYXJjaE5hbWUgPSBgSWRlbnRpZnkgJHtyZWNvcmRDbGFzcy5nZXQoJ2Rpc3BsYXlOYW1lUGx1cmFsJyl9IGJ5ICR7cXVlc3Rpb24uZ2V0KCdkaXNwbGF5TmFtZScpfWA7XG4gICAgICByZXR1cm4gKFxuICAgICAgICA8bGkga2V5PXtpbmRleH0+XG4gICAgICAgICAgPGEgaHJlZj17Jy9hL3Nob3dRdWVzdGlvbi5kbz9xdWVzdGlvbkZ1bGxOYW1lPScgKyBuYW1lfT57c2VhcmNoTmFtZX08L2E+XG4gICAgICAgIDwvbGk+XG4gICAgICApO1xuICAgIH1cbiAgfSk7XG5cbiAgdmFyIExpbmtzID0gUmVhY3QuY3JlYXRlQ2xhc3Moe1xuICAgIHJlbmRlcigpIHtcbiAgICAgIHZhciB7IGxpbmtzIH0gPSB0aGlzLnByb3BzO1xuXG4gICAgICBpZiAobGlua3MuZ2V0KCdyb3dzJykuc2l6ZSA9PT0gMCkgcmV0dXJuIG51bGw7XG5cbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxkaXY+XG4gICAgICAgICAgPGgyPkxpbmtzPC9oMj5cbiAgICAgICAgICA8dWw+IHtsaW5rcy5nZXQoJ3Jvd3MnKS5tYXAodGhpcy5fcmVuZGVyTGluaykudG9BcnJheSgpfSA8L3VsPlxuICAgICAgICA8L2Rpdj5cbiAgICAgICk7XG4gICAgfSxcblxuICAgIF9yZW5kZXJMaW5rKGxpbmssIGluZGV4KSB7XG4gICAgICB2YXIgaHlwZXJMaW5rID0gbGluay5maW5kKGF0dHIgPT4gYXR0ci5nZXQoJ25hbWUnKSA9PSAnaHlwZXJfbGluaycpO1xuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGxpIGtleT17aW5kZXh9Pntmb3JtYXRMaW5rKGh5cGVyTGluay5nZXQoJ3ZhbHVlJykpfTwvbGk+XG4gICAgICApO1xuICAgIH1cbiAgfSk7XG5cbiAgdmFyIENvbnRhY3RzID0gUmVhY3QuY3JlYXRlQ2xhc3Moe1xuICAgIHJlbmRlcigpIHtcbiAgICAgIHZhciB7IGNvbnRhY3RzIH0gPSB0aGlzLnByb3BzO1xuICAgICAgaWYgKGNvbnRhY3RzLmdldCgncm93cycpLnNpemUgPT09IDApIHJldHVybiBudWxsO1xuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGRpdj5cbiAgICAgICAgICA8aDQ+Q29udGFjdHM8L2g0PlxuICAgICAgICAgIDx1bD5cbiAgICAgICAgICAgIHtjb250YWN0cy5nZXQoJ3Jvd3MnKS5tYXAodGhpcy5fcmVuZGVyQ29udGFjdCkudG9BcnJheSgpfVxuICAgICAgICAgIDwvdWw+XG4gICAgICAgIDwvZGl2PlxuICAgICAgKTtcbiAgICB9LFxuXG4gICAgX3JlbmRlckNvbnRhY3QoY29udGFjdCwgaW5kZXgpIHtcbiAgICAgIHZhciBjb250YWN0X25hbWUgPSBjb250YWN0LmZpbmQoYyA9PiBjLmdldCgnbmFtZScpID09ICdjb250YWN0X25hbWUnKTtcbiAgICAgIHZhciBhZmZpbGlhdGlvbiA9IGNvbnRhY3QuZmluZChjID0+IGMuZ2V0KCduYW1lJykgPT0gJ2FmZmlsaWF0aW9uJyk7XG4gICAgICByZXR1cm4gKFxuICAgICAgICA8bGkga2V5PXtpbmRleH0+e2NvbnRhY3RfbmFtZS5nZXQoJ3ZhbHVlJyl9LCB7YWZmaWxpYXRpb24uZ2V0KCd2YWx1ZScpfTwvbGk+XG4gICAgICApO1xuICAgIH1cbiAgfSk7XG5cbiAgdmFyIFB1YmxpY2F0aW9ucyA9IFJlYWN0LmNyZWF0ZUNsYXNzKHtcbiAgICByZW5kZXIoKSB7XG4gICAgICB2YXIgeyBwdWJsaWNhdGlvbnMgfSA9IHRoaXMucHJvcHM7XG4gICAgICB2YXIgcm93cyA9IHB1YmxpY2F0aW9ucy5nZXQoJ3Jvd3MnKTtcbiAgICAgIGlmIChyb3dzLnNpemUgPT09IDApIHJldHVybiBudWxsO1xuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGRpdj5cbiAgICAgICAgICA8aDQ+UHVibGljYXRpb25zPC9oND5cbiAgICAgICAgICA8dWw+e3Jvd3MubWFwKHRoaXMuX3JlbmRlclB1YmxpY2F0aW9uKS50b0FycmF5KCl9PC91bD5cbiAgICAgICAgPC9kaXY+XG4gICAgICApO1xuICAgIH0sXG5cbiAgICBfcmVuZGVyUHVibGljYXRpb24ocHVibGljYXRpb24sIGluZGV4KSB7XG4gICAgICB2YXIgcHVibWVkX2xpbmsgPSBwdWJsaWNhdGlvbi5maW5kKHAgPT4gcC5nZXQoJ25hbWUnKSA9PSAncHVibWVkX2xpbmsnKTtcbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxsaSBrZXk9e2luZGV4fT57Zm9ybWF0TGluayhwdWJtZWRfbGluay5nZXQoJ3ZhbHVlJykpfTwvbGk+XG4gICAgICApO1xuICAgIH1cbiAgfSk7XG5cbiAgdmFyIENvbnRhY3RzQW5kUHVibGljYXRpb25zID0gUmVhY3QuY3JlYXRlQ2xhc3Moe1xuICAgIHJlbmRlcigpIHtcbiAgICAgIHZhciB7IGNvbnRhY3RzLCBwdWJsaWNhdGlvbnMgfSA9IHRoaXMucHJvcHM7XG5cbiAgICAgIGlmIChjb250YWN0cy5nZXQoJ3Jvd3MnKS5zaXplID09PSAwICYmIHB1YmxpY2F0aW9ucy5nZXQoJ3Jvd3MnKS5zaXplID09PSAwKSByZXR1cm4gbnVsbDtcblxuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGRpdj5cbiAgICAgICAgICA8aDI+QWRkaXRpb25hbCBDb250YWN0cyBhbmQgUHVibGljYXRpb25zPC9oMj5cbiAgICAgICAgICA8Q29udGFjdHMgY29udGFjdHM9e2NvbnRhY3RzfS8+XG4gICAgICAgICAgPFB1YmxpY2F0aW9ucyBwdWJsaWNhdGlvbnM9e3B1YmxpY2F0aW9uc30vPlxuICAgICAgICA8L2Rpdj5cbiAgICAgICk7XG4gICAgfVxuICB9KTtcblxuICB2YXIgUmVsZWFzZUhpc3RvcnkgPSBSZWFjdC5jcmVhdGVDbGFzcyh7XG4gICAgcmVuZGVyKCkge1xuICAgICAgdmFyIHsgaGlzdG9yeSB9ID0gdGhpcy5wcm9wcztcbiAgICAgIGlmIChoaXN0b3J5LmdldCgncm93cycpLnNpemUgPT09IDApIHJldHVybiBudWxsO1xuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGRpdj5cbiAgICAgICAgICA8aDI+RGF0YSBTZXQgUmVsZWFzZSBIaXN0b3J5PC9oMj5cbiAgICAgICAgICA8dGFibGU+XG4gICAgICAgICAgICA8dGhlYWQ+XG4gICAgICAgICAgICAgIDx0cj5cbiAgICAgICAgICAgICAgICA8dGg+RXVQYXRoREIgUmVsZWFzZTwvdGg+XG4gICAgICAgICAgICAgICAgPHRoPkdlbm9tZSBTb3VyY2U8L3RoPlxuICAgICAgICAgICAgICAgIDx0aD5Bbm5vdGF0aW9uIFNvdXJjZTwvdGg+XG4gICAgICAgICAgICAgICAgPHRoPk5vdGVzPC90aD5cbiAgICAgICAgICAgICAgPC90cj5cbiAgICAgICAgICAgIDwvdGhlYWQ+XG4gICAgICAgICAgICA8dGJvZHk+XG4gICAgICAgICAgICAgIHtoaXN0b3J5LmdldCgncm93cycpLm1hcCh0aGlzLl9yZW5kZXJSb3cpLnRvQXJyYXkoKX1cbiAgICAgICAgICAgIDwvdGJvZHk+XG4gICAgICAgICAgPC90YWJsZT5cbiAgICAgICAgPC9kaXY+XG4gICAgICApO1xuICAgIH0sXG5cbiAgICBfcmVuZGVyUm93KGF0dHJpYnV0ZXMpIHtcbiAgICAgIHZhciBhdHRycyA9IF8uaW5kZXhCeShhdHRyaWJ1dGVzLnRvSlMoKSwgJ25hbWUnKTtcblxuICAgICAgdmFyIHJlbGVhc2VEYXRlID0gYXR0cnMucmVsZWFzZV9kYXRlLnZhbHVlLnNwbGl0KC9cXHMrLylbMF07XG5cbiAgICAgIHZhciByZWxlYXNlID0gYXR0cnMuYnVpbGQudmFsdWUgPT0gMFxuICAgICAgICA/ICdJbml0aWFsIHJlbGVhc2UnXG4gICAgICAgIDogYCR7YXR0cnMucHJvamVjdC52YWx1ZX0gJHthdHRycy5yZWxlYXNlX251bWJlci52YWx1ZX0gJHtyZWxlYXNlRGF0ZX1gO1xuXG4gICAgICB2YXIgZ2Vub21lU291cmNlID0gYXR0cnMuZ2Vub21lX3NvdXJjZS52YWx1ZVxuICAgICAgICA/IGF0dHJzLmdlbm9tZV9zb3VyY2UudmFsdWUgKyAnICgnICsgYXR0cnMuZ2Vub21lX3ZlcnNpb24udmFsdWUgKyAnKSdcbiAgICAgICAgOiAnJztcblxuICAgICAgdmFyIGFubm90YXRpb25Tb3VyY2UgPSBhdHRycy5hbm5vdGF0aW9uX3NvdXJjZS52YWx1ZVxuICAgICAgICA/IGF0dHJzLmFubm90YXRpb25fc291cmNlLnZhbHVlICsgJyAoJyArIGF0dHJzLmFubm90YXRpb25fdmVyc2lvbi52YWx1ZSArICcpJ1xuICAgICAgICA6ICcnO1xuXG4gICAgICByZXR1cm4gKFxuICAgICAgICA8dHI+XG4gICAgICAgICAgPHRkPntyZWxlYXNlfTwvdGQ+XG4gICAgICAgICAgPHRkPntnZW5vbWVTb3VyY2V9PC90ZD5cbiAgICAgICAgICA8dGQ+e2Fubm90YXRpb25Tb3VyY2V9PC90ZD5cbiAgICAgICAgICA8dGQ+e2F0dHJzLm5vdGUudmFsdWV9PC90ZD5cbiAgICAgICAgPC90cj5cbiAgICAgICk7XG4gICAgfVxuICB9KTtcblxuICB2YXIgVmVyc2lvbnMgPSBSZWFjdC5jcmVhdGVDbGFzcyh7XG4gICAgcmVuZGVyKCkge1xuICAgICAgdmFyIHsgdmVyc2lvbnMgfSA9IHRoaXMucHJvcHM7XG4gICAgICB2YXIgcm93cyA9IHZlcnNpb25zLmdldCgncm93cycpO1xuXG4gICAgICBpZiAocm93cy5zaXplID09PSAwKSByZXR1cm4gbnVsbDtcblxuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGRpdj5cbiAgICAgICAgICA8aDI+UHJvdmlkZXIncyBWZXJzaW9uPC9oMj5cbiAgICAgICAgICA8cD5cbiAgICAgICAgICAgIFRoZSBkYXRhIHNldCB2ZXJzaW9uIHNob3duIGhlcmUgaXMgdGhlIGRhdGEgcHJvdmlkZXIncyB2ZXJzaW9uXG4gICAgICAgICAgICBudW1iZXIgb3IgcHVibGljYXRpb24gZGF0ZSBpbmRpY2F0ZWQgb24gdGhlIHNpdGUgZnJvbSB3aGljaCB3ZVxuICAgICAgICAgICAgZG93bmxvYWRlZCB0aGUgZGF0YS4gSW4gdGhlIHJhcmUgY2FzZSB0aGF0IHRoZXNlIGFyZSBub3QgYXZhaWxhYmxlLFxuICAgICAgICAgICAgdGhlIHZlcnNpb24gaXMgdGhlIGRhdGUgdGhhdCB0aGUgZGF0YSBzZXQgd2FzIGRvd25sb2FkZWQuXG4gICAgICAgICAgPC9wPlxuICAgICAgICAgIDx0YWJsZT5cbiAgICAgICAgICAgIDx0aGVhZD5cbiAgICAgICAgICAgICAgPHRyPlxuICAgICAgICAgICAgICAgIDx0aD5PcmdhbmlzbTwvdGg+XG4gICAgICAgICAgICAgICAgPHRoPlByb3ZpZGVyJ3MgVmVyc2lvbjwvdGg+XG4gICAgICAgICAgICAgIDwvdHI+XG4gICAgICAgICAgICA8L3RoZWFkPlxuICAgICAgICAgICAgPHRib2R5PlxuICAgICAgICAgICAgICB7cm93cy5tYXAodGhpcy5fcmVuZGVyUm93KS50b0FycmF5KCl9XG4gICAgICAgICAgICA8L3Rib2R5PlxuICAgICAgICAgIDwvdGFibGU+XG4gICAgICAgIDwvZGl2PlxuICAgICAgKTtcbiAgICB9LFxuXG4gICAgX3JlbmRlclJvdyhhdHRyaWJ1dGVzKSB7XG4gICAgICB2YXIgYXR0cnMgPSBfLmluZGV4QnkoYXR0cmlidXRlcy50b0pTKCksICduYW1lJyk7XG4gICAgICByZXR1cm4gKFxuICAgICAgICA8dHI+XG4gICAgICAgICAgPHRkPnthdHRycy5vcmdhbmlzbS52YWx1ZX08L3RkPlxuICAgICAgICAgIDx0ZD57YXR0cnMudmVyc2lvbi52YWx1ZX08L3RkPlxuICAgICAgICA8L3RyPlxuICAgICAgKTtcbiAgICB9XG4gIH0pO1xuXG4gIHZhciBHcmFwaHMgPSBSZWFjdC5jcmVhdGVDbGFzcyh7XG4gICAgcmVuZGVyKCkge1xuICAgICAgdmFyIHsgZ3JhcGhzIH0gPSB0aGlzLnByb3BzO1xuICAgICAgdmFyIHJvd3MgPSBncmFwaHMuZ2V0KCdyb3dzJyk7XG4gICAgICBpZiAocm93cy5zaXplID09PSAwKSByZXR1cm4gbnVsbDtcbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxkaXY+XG4gICAgICAgICAgPGgyPkV4YW1wbGUgR3JhcGhzPC9oMj5cbiAgICAgICAgICA8dWw+e3Jvd3MubWFwKHRoaXMuX3JlbmRlckdyYXBoKS50b0FycmF5KCl9PC91bD5cbiAgICAgICAgPC9kaXY+XG4gICAgICApO1xuICAgIH0sXG5cbiAgICBfcmVuZGVyR3JhcGgoZ3JhcGgsIGluZGV4KSB7XG4gICAgICB2YXIgZyA9IF8uaW5kZXhCeShncmFwaC50b0pTKCksICduYW1lJyk7XG5cbiAgICAgIHZhciBkaXNwbGF5TmFtZSA9IGcuZGlzcGxheV9uYW1lLnZhbHVlO1xuXG4gICAgICB2YXIgYmFzZVVybCA9ICcvY2dpLWJpbi9kYXRhUGxvdHRlci5wbCcgK1xuICAgICAgICAnP3R5cGU9JyArIGcubW9kdWxlLnZhbHVlICtcbiAgICAgICAgJyZwcm9qZWN0X2lkPScgKyBnLnByb2plY3RfaWQudmFsdWUgK1xuICAgICAgICAnJmRhdGFzZXQ9JyArIGcuZGF0YXNldF9uYW1lLnZhbHVlICtcbiAgICAgICAgJyZ0ZW1wbGF0ZT0nICsgKGcuaXNfZ3JhcGhfY3VzdG9tLnZhbHVlID09PSAnZmFsc2UnID8gMSA6ICcnKSArXG4gICAgICAgICcmaWQ9JyArIGcuZ3JhcGhfaWRzLnZhbHVlO1xuXG4gICAgICB2YXIgaW1nVXJsID0gYmFzZVVybCArICcmZm10PXBuZyc7XG4gICAgICB2YXIgdGFibGVVcmwgPSBiYXNlVXJsICsgJyZmbXQ9dGFibGUnO1xuXG4gICAgICByZXR1cm4gKFxuICAgICAgICA8bGkga2V5PXtpbmRleH0+XG4gICAgICAgICAgPGgzPntkaXNwbGF5TmFtZX08L2gzPlxuICAgICAgICAgIDxkaXYgY2xhc3NOYW1lPVwiZXVwYXRoZGItRGF0YXNldFJlY29yZC1HcmFwaE1ldGFcIj5cbiAgICAgICAgICAgIDxoMz5EZXNjcmlwdGlvbjwvaDM+XG4gICAgICAgICAgICA8cCBkYW5nZXJvdXNseVNldElubmVySFRNTD17e19faHRtbDogZy5kZXNjcmlwdGlvbi52YWx1ZX19Lz5cbiAgICAgICAgICAgIDxoMz5YLWF4aXM8L2gzPlxuICAgICAgICAgICAgPHA+e2cueF9heGlzLnZhbHVlfTwvcD5cbiAgICAgICAgICAgIDxoMz5ZLWF4aXM8L2gzPlxuICAgICAgICAgICAgPHA+e2cueV9heGlzLnZhbHVlfTwvcD5cbiAgICAgICAgICA8L2Rpdj5cbiAgICAgICAgICA8ZGl2IGNsYXNzTmFtZT1cImV1cGF0aGRiLURhdGFzZXRSZWNvcmQtR3JhcGhEYXRhXCI+XG4gICAgICAgICAgICA8aW1nIGNsYXNzTmFtZT1cImV1cGF0aGRiLURhdGFzZXRSZWNvcmQtR3JhcGhJbWdcIiBzcmM9e2ltZ1VybH0vPlxuICAgICAgICAgIDwvZGl2PlxuICAgICAgICA8L2xpPlxuICAgICAgKTtcbiAgICB9XG4gIH0pO1xuXG4gIHZhciBJc29sYXRlc0xpc3QgPSBSZWFjdC5jcmVhdGVDbGFzcyh7XG5cbiAgICByZW5kZXIoKSB7XG4gICAgICB2YXIgeyByb3dzIH0gPSB0aGlzLnByb3BzLmlzb2xhdGVzLnRvSlMoKTtcblxuICAgICAgaWYgKHJvd3MubGVuZ3RoID09PSAwKSByZXR1cm4gbnVsbDtcblxuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGRpdj5cbiAgICAgICAgICA8aDI+SXNvbGF0ZXMgLyBTYW1wbGVzPC9oMj5cbiAgICAgICAgICA8dWw+XG4gICAgICAgICAgICB7cm93cy5tYXAodGhpcy5fcmVuZGVyUm93KX1cbiAgICAgICAgICA8L3VsPlxuICAgICAgICA8L2Rpdj5cbiAgICAgICk7XG4gICAgfSxcblxuICAgIF9yZW5kZXJSb3coYXR0cmlidXRlcywgaW5kZXgpIHtcbiAgICAgIHZhciBpc29sYXRlX2xpbmsgPSBhdHRyaWJ1dGVzLmZpbmQoYXR0ciA9PiBhdHRyLm5hbWUgPT09ICdpc29sYXRlX2xpbmsnKTtcbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxsaSBrZXk9e2luZGV4fT57Zm9ybWF0TGluayhpc29sYXRlX2xpbmsudmFsdWUpfTwvbGk+XG4gICAgICApO1xuICAgIH1cblxuICB9KTtcblxuICB2YXIgRGF0YXNldFJlY29yZCA9IFJlYWN0LmNyZWF0ZUNsYXNzKHtcbiAgICByZW5kZXIoKSB7XG4gICAgICB2YXIgeyByZWNvcmQsIHF1ZXN0aW9ucywgcmVjb3JkQ2xhc3NlcyB9ID0gdGhpcy5wcm9wcztcbiAgICAgIHZhciBhdHRyaWJ1dGVzID0gcmVjb3JkLmdldCgnYXR0cmlidXRlcycpO1xuICAgICAgdmFyIHRhYmxlcyA9IHJlY29yZC5nZXQoJ3RhYmxlcycpO1xuICAgICAgdmFyIHRpdGxlQ2xhc3MgPSAnZXVwYXRoZGItRGF0YXNldFJlY29yZC10aXRsZSc7XG5cbiAgICAgIHZhciBpZCA9IHJlY29yZC5nZXQoJ2lkJyk7XG4gICAgICB2YXIgc3VtbWFyeSA9IGF0dHJpYnV0ZXMuZ2V0SW4oWydzdW1tYXJ5JywgJ3ZhbHVlJ10pO1xuICAgICAgdmFyIHJlbGVhc2VJbmZvID0gYXR0cmlidXRlcy5nZXRJbihbJ2V1cGF0aF9yZWxlYXNlJywgJ3ZhbHVlJ10pO1xuICAgICAgdmFyIHByaW1hcnlQdWJsaWNhdGlvbiA9IHRhYmxlcy5nZXRJbihbJ1B1YmxpY2F0aW9ucycsICdyb3dzJywgMF0pO1xuICAgICAgdmFyIGNvbnRhY3QgPSBhdHRyaWJ1dGVzLmdldEluKFsnY29udGFjdCcsICd2YWx1ZSddKTtcbiAgICAgIHZhciBpbnN0aXR1dGlvbiA9IGF0dHJpYnV0ZXMuZ2V0SW4oWydpbnN0aXR1dGlvbicsICd2YWx1ZSddKTtcbiAgICAgIHZhciB2ZXJzaW9uID0gdGFibGVzLmdldEluKFsnVmVyc2lvbicsICdyb3dzJywgMF0pO1xuICAgICAgdmFyIG9yZ2FuaXNtID0gYXR0cmlidXRlcy5nZXRJbihbJ29yZ2FuaXNtX3ByZWZpeCcsICd2YWx1ZSddKTtcbiAgICAgIHZhciBvcmdhbmlzbXMgPSBhdHRyaWJ1dGVzLmdldEluKFsnb3JnYW5pc21zJywgJ3ZhbHVlJ10pO1xuICAgICAgdmFyIFJlZmVyZW5jZXMgPSB0YWJsZXMuZ2V0KCdSZWZlcmVuY2VzJyk7XG4gICAgICB2YXIgSHlwZXJMaW5rcyA9IHRhYmxlcy5nZXQoJ0h5cGVyTGlua3MnKTtcbiAgICAgIHZhciBDb250YWN0cyA9IHRhYmxlcy5nZXQoJ0NvbnRhY3RzJyk7XG4gICAgICB2YXIgUHVibGljYXRpb25zID0gdGFibGVzLmdldCgnUHVibGljYXRpb25zJyk7XG4gICAgICB2YXIgZGVzY3JpcHRpb24gPSBhdHRyaWJ1dGVzLmdldEluKFsnZGVzY3JpcHRpb24nLCAndmFsdWUnXSk7XG4gICAgICB2YXIgR2Vub21lSGlzdG9yeSA9IHRhYmxlcy5nZXQoJ0dlbm9tZUhpc3RvcnknKTtcbiAgICAgIHZhciBWZXJzaW9uID0gdGFibGVzLmdldCgnVmVyc2lvbicpO1xuICAgICAgdmFyIEV4YW1wbGVHcmFwaHMgPSB0YWJsZXMuZ2V0KCdFeGFtcGxlR3JhcGhzJyk7XG4gICAgICB2YXIgSXNvbGF0ZXMgPSB0YWJsZXMuZ2V0KCdJc29sYXRlcycpO1xuXG4gICAgICByZXR1cm4gKFxuICAgICAgICA8ZGl2IGNsYXNzTmFtZT1cImV1cGF0aGRiLURhdGFzZXRSZWNvcmQgdWktaGVscGVyLWNsZWFyZml4XCI+XG4gICAgICAgICAgPGgxIGRhbmdlcm91c2x5U2V0SW5uZXJIVE1MPXt7XG4gICAgICAgICAgICBfX2h0bWw6ICdEYXRhIFNldDogPHNwYW4gY2xhc3M9XCInICsgdGl0bGVDbGFzcyArICdcIj4nICsgaWQgKyAnPC9zcGFuPidcbiAgICAgICAgICB9fS8+XG5cbiAgICAgICAgICA8ZGl2IGNsYXNzTmFtZT1cImV1cGF0aGRiLURhdGFzZXRSZWNvcmQtQ29udGFpbmVyIHVpLWhlbHBlci1jbGVhcmZpeFwiPlxuXG4gICAgICAgICAgICA8aHIvPlxuXG4gICAgICAgICAgICA8dGFibGUgY2xhc3NOYW1lPVwiZXVwYXRoZGItRGF0YXNldFJlY29yZC1oZWFkZXJUYWJsZVwiPlxuICAgICAgICAgICAgICA8dGJvZHk+XG5cbiAgICAgICAgICAgICAgICA8dHI+XG4gICAgICAgICAgICAgICAgICA8dGg+U3VtbWFyeTo8L3RoPlxuICAgICAgICAgICAgICAgICAgPHRkIGRhbmdlcm91c2x5U2V0SW5uZXJIVE1MPXt7X19odG1sOiBzdW1tYXJ5fX0vPlxuICAgICAgICAgICAgICAgIDwvdHI+XG5cbiAgICAgICAgICAgICAgICB7b3JnYW5pc20gPyAoXG4gICAgICAgICAgICAgICAgICA8dHI+XG4gICAgICAgICAgICAgICAgICAgIDx0aD5PcmdhbmlzbSAoc291cmNlIG9yIHJlZmVyZW5jZSk6PC90aD5cbiAgICAgICAgICAgICAgICAgICAgPHRkIGRhbmdlcm91c2x5U2V0SW5uZXJIVE1MPXt7X19odG1sOiBvcmdhbmlzbX19Lz5cbiAgICAgICAgICAgICAgICAgIDwvdHI+XG4gICAgICAgICAgICAgICAgKSA6IG51bGx9XG5cbiAgICAgICAgICAgICAgICB7cHJpbWFyeVB1YmxpY2F0aW9uID8gKFxuICAgICAgICAgICAgICAgICAgPHRyPlxuICAgICAgICAgICAgICAgICAgICA8dGg+UHJpbWFyeSBwdWJsaWNhdGlvbjo8L3RoPlxuICAgICAgICAgICAgICAgICAgICA8dGQ+e3JlbmRlclByaW1hcnlQdWJsaWNhdGlvbihwcmltYXJ5UHVibGljYXRpb24pfTwvdGQ+XG4gICAgICAgICAgICAgICAgICA8L3RyPlxuICAgICAgICAgICAgICAgICkgOiBudWxsfVxuXG4gICAgICAgICAgICAgICAge2NvbnRhY3QgJiYgaW5zdGl0dXRpb24gPyAoXG4gICAgICAgICAgICAgICAgICA8dHI+XG4gICAgICAgICAgICAgICAgICAgIDx0aD5QcmltYXJ5IGNvbnRhY3Q6PC90aD5cbiAgICAgICAgICAgICAgICAgICAgPHRkPntyZW5kZXJQcmltYXJ5Q29udGFjdChjb250YWN0LCBpbnN0aXR1dGlvbil9PC90ZD5cbiAgICAgICAgICAgICAgICAgIDwvdHI+XG4gICAgICAgICAgICAgICAgKSA6IG51bGx9XG5cbiAgICAgICAgICAgICAgICB7dmVyc2lvbiA/IChcbiAgICAgICAgICAgICAgICAgIDx0cj5cbiAgICAgICAgICAgICAgICAgICAgPHRoPlNvdXJjZSB2ZXJzaW9uOjwvdGg+XG4gICAgICAgICAgICAgICAgICAgIDx0ZD57cmVuZGVyU291cmNlVmVyc2lvbih2ZXJzaW9uKX08L3RkPlxuICAgICAgICAgICAgICAgICAgPC90cj5cbiAgICAgICAgICAgICAgICApIDogbnVsbH1cblxuICAgICAgICAgICAgICAgIHtyZWxlYXNlSW5mbyA/IChcbiAgICAgICAgICAgICAgICAgIDx0cj5cbiAgICAgICAgICAgICAgICAgICAgPHRoPkV1UGF0aERCIHJlbGVhc2UgIyAvIGRhdGU6PC90aD5cbiAgICAgICAgICAgICAgICAgICAgPHRkPntyZWxlYXNlSW5mb308L3RkPlxuICAgICAgICAgICAgICAgICAgPC90cj5cbiAgICAgICAgICAgICAgICApIDogbnVsbH1cblxuICAgICAgICAgICAgICA8L3Rib2R5PlxuICAgICAgICAgICAgPC90YWJsZT5cblxuICAgICAgICAgICAgPGhyLz5cblxuICAgICAgICAgICAgPGRpdiBjbGFzc05hbWU9XCJldXBhdGhkYi1EYXRhc2V0UmVjb3JkLU1haW5cIj5cbiAgICAgICAgICAgICAgPGgyPkRldGFpbGVkIERlc2NyaXB0aW9uPC9oMj5cbiAgICAgICAgICAgICAgPGRpdiBkYW5nZXJvdXNseVNldElubmVySFRNTD17e19faHRtbDogZGVzY3JpcHRpb259fS8+XG4gICAgICAgICAgICAgIDxDb250YWN0c0FuZFB1YmxpY2F0aW9ucyBjb250YWN0cz17Q29udGFjdHN9IHB1YmxpY2F0aW9ucz17UHVibGljYXRpb25zfS8+XG4gICAgICAgICAgICA8L2Rpdj5cblxuICAgICAgICAgICAgPGRpdiBjbGFzc05hbWU9XCJldXBhdGhkYi1EYXRhc2V0UmVjb3JkLVNpZGViYXJcIj5cbiAgICAgICAgICAgICAgPE9yZ2FuaXNtcyBvcmdhbmlzbXM9e29yZ2FuaXNtc30vPlxuICAgICAgICAgICAgICA8U2VhcmNoZXMgc2VhcmNoZXM9e1JlZmVyZW5jZXN9IHF1ZXN0aW9ucz17cXVlc3Rpb25zfSByZWNvcmRDbGFzc2VzPXtyZWNvcmRDbGFzc2VzfS8+XG4gICAgICAgICAgICAgIDxJc29sYXRlc0xpc3QgaXNvbGF0ZXM9e0lzb2xhdGVzfS8+XG4gICAgICAgICAgICAgIDxMaW5rcyBsaW5rcz17SHlwZXJMaW5rc30vPlxuICAgICAgICAgICAgICA8UmVsZWFzZUhpc3RvcnkgaGlzdG9yeT17R2Vub21lSGlzdG9yeX0vPlxuICAgICAgICAgICAgICA8VmVyc2lvbnMgdmVyc2lvbnM9e1ZlcnNpb259Lz5cbiAgICAgICAgICAgIDwvZGl2PlxuXG4gICAgICAgICAgPC9kaXY+XG4gICAgICAgICAgPEdyYXBocyBncmFwaHM9e0V4YW1wbGVHcmFwaHN9Lz5cbiAgICAgICAgPC9kaXY+XG4gICAgICApO1xuICAgIH1cbiAgfSk7XG5cbiAgdmFyIFRvb2x0aXAgPSBSZWFjdC5jcmVhdGVDbGFzcyh7XG4gICAgY29tcG9uZW50RGlkTW91bnQoKSB7XG4gICAgICB0aGlzLl9zZXR1cFRvb2x0aXAoKTtcbiAgICB9LFxuICAgIGNvbXBvbmVudERpZFVwZGF0ZSgpIHtcbiAgICAgIHRoaXMuX2Rlc3Ryb3lUb29sdGlwKCk7XG4gICAgICB0aGlzLl9zZXR1cFRvb2x0aXAoKTtcbiAgICB9LFxuICAgIGNvbXBvbmVudFdpbGxVbm1vdW50KCkge1xuICAgICAgdGhpcy5fZGVzdHJveVRvb2x0aXAoKTtcbiAgICB9LFxuICAgIF9zZXR1cFRvb2x0aXAoKSB7XG4gICAgICBpZiAodGhpcy5wcm9wcy50ZXh0ID09IG51bGwpIHJldHVybjtcblxuICAgICAgdmFyIHRleHQgPSBgPGRpdiBzdHlsZT1cIm1heC1oZWlnaHQ6IDIwMHB4OyBvdmVyZmxvdy15OiBhdXRvOyBwYWRkaW5nOiAycHg7XCI+JHt0aGlzLnByb3BzLnRleHR9PC9kaXY+YDtcbiAgICAgIHZhciB3aWR0aCA9IHRoaXMucHJvcHMud2lkdGg7XG5cbiAgICAgIHRoaXMuJHRhcmdldCA9ICQodGhpcy5nZXRET01Ob2RlKCkpLmZpbmQoJy53ZGstUmVjb3JkVGFibGUtcmVjb3JkTGluaycpXG4gICAgICAgIC53ZGtUb29sdGlwKHtcbiAgICAgICAgICBvdmVyd3JpdGU6IHRydWUsXG4gICAgICAgICAgY29udGVudDogeyB0ZXh0IH0sXG4gICAgICAgICAgc2hvdzogeyBkZWxheTogMTAwMCB9LFxuICAgICAgICAgIHBvc2l0aW9uOiB7IG15OiAndG9wIGxlZnQnLCBhdDogJ2JvdHRvbSBsZWZ0JywgYWRqdXN0OiB7IHk6IDEyIH0gfVxuICAgICAgICB9KTtcbiAgICB9LFxuICAgIF9kZXN0cm95VG9vbHRpcCgpIHtcbiAgICAgIC8vIGlmIF9zZXR1cFRvb2x0aXAgZG9lc24ndCBkbyBhbnl0aGluZywgdGhpcyBpcyBhIG5vb3BcbiAgICAgIGlmICh0aGlzLiR0YXJnZXQpIHtcbiAgICAgICAgdGhpcy4kdGFyZ2V0LnF0aXAoJ2Rlc3Ryb3knLCB0cnVlKTtcbiAgICAgIH1cbiAgICB9LFxuICAgIHJlbmRlcigpIHtcbiAgICAgIC8vIEZJWE1FIC0gRmlndXJlIG91dCB3aHkgd2UgbG9zZSB0aGUgZml4ZWQtZGF0YS10YWJsZSBjbGFzc05hbWVcbiAgICAgIC8vIExvc2luZyB0aGUgZml4ZWQtZGF0YS10YWJsZSBjbGFzc05hbWUgZm9yIHNvbWUgcmVhc29uLi4uIGFkZGluZyBpdCBiYWNrLlxuICAgICAgdmFyIGNoaWxkID0gUmVhY3QuQ2hpbGRyZW4ub25seSh0aGlzLnByb3BzLmNoaWxkcmVuKTtcbiAgICAgIGNoaWxkLnByb3BzLmNsYXNzTmFtZSArPSBcIiBwdWJsaWNfZml4ZWREYXRhVGFibGVDZWxsX2NlbGxDb250ZW50XCI7XG4gICAgICByZXR1cm4gY2hpbGQ7XG4gICAgICAvL3JldHVybiB0aGlzLnByb3BzLmNoaWxkcmVuO1xuICAgIH1cbiAgfSk7XG5cbiAgZnVuY3Rpb24gZGF0YXNldENlbGxSZW5kZXJlcihhdHRyaWJ1dGUsIGF0dHJpYnV0ZU5hbWUsIGF0dHJpYnV0ZXMsIGluZGV4LCBjb2x1bW5EYXRhLCB3aWR0aCwgZGVmYXVsdFJlbmRlcmVyKSB7XG4gICAgdmFyIHJlYWN0RWxlbWVudCA9IGRlZmF1bHRSZW5kZXJlcihhdHRyaWJ1dGUsIGF0dHJpYnV0ZU5hbWUsIGF0dHJpYnV0ZXMsIGluZGV4LCBjb2x1bW5EYXRhLCB3aWR0aCk7XG5cbiAgICBpZiAoYXR0cmlidXRlLmdldCgnbmFtZScpID09PSAncHJpbWFyeV9rZXknKSB7XG4gICAgICByZXR1cm4gKFxuICAgICAgICA8VG9vbHRpcFxuICAgICAgICAgIHRleHQ9e2F0dHJpYnV0ZXMuZ2V0KCdkZXNjcmlwdGlvbicpLmdldCgndmFsdWUnKX1cbiAgICAgICAgICB3aWR0aD17d2lkdGh9XG4gICAgICAgID57cmVhY3RFbGVtZW50fTwvVG9vbHRpcD5cbiAgICAgICk7XG4gICAgfVxuICAgIGVsc2Uge1xuICAgICAgcmV0dXJuIHJlYWN0RWxlbWVudDtcbiAgICB9XG4gIH1cblxuICBucy5EYXRhc2V0UmVjb3JkID0gRGF0YXNldFJlY29yZDtcbiAgbnMuZGF0YXNldENlbGxSZW5kZXJlciA9IGRhdGFzZXRDZWxsUmVuZGVyZXI7XG59KTtcbiJdfQ==