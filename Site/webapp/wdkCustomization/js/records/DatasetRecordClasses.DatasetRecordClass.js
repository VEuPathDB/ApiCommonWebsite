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

//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIkRhdGFzZXRSZWNvcmRDbGFzc2VzLkRhdGFzZXRSZWNvcmRDbGFzcy5qc3giXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7Ozs7Ozs7Ozs7Ozs7OztBQWNBLEdBQUcsQ0FBQyxTQUFTLENBQUMsa0JBQWtCLEVBQUUsVUFBUyxFQUFFLEVBQUU7QUFDN0MsY0FBWSxDQUFDOztBQUViLE1BQUksS0FBSyxHQUFHLEdBQUcsQ0FBQyxLQUFLLENBQUM7OztBQUd0QixXQUFTLFFBQVEsQ0FBQyxHQUFHLEVBQUU7QUFDckIsUUFBSSxHQUFHLEdBQUcsUUFBUSxDQUFDLGFBQWEsQ0FBQyxLQUFLLENBQUMsQ0FBQztBQUN4QyxPQUFHLENBQUMsU0FBUyxHQUFHLEdBQUcsQ0FBQztBQUNwQixXQUFPLEdBQUcsQ0FBQyxXQUFXLENBQUM7R0FDeEI7OztBQUdELE1BQUksVUFBVSxHQUFHLFNBQVMsVUFBVSxDQUFDLElBQUksRUFBRSxJQUFJLEVBQUU7QUFDL0MsUUFBSSxHQUFHLElBQUksSUFBSSxFQUFFLENBQUM7QUFDbEIsUUFBSSxTQUFTLEdBQUcsQ0FBQyxDQUFDLElBQUksQ0FBQyxTQUFTLENBQUM7QUFDakMsUUFBSSxLQUFLLEdBQUcsY0FBYyxDQUFDLElBQUksQ0FBQyxJQUFJLENBQUMsT0FBTyxDQUFDLEtBQUssRUFBRSxHQUFHLENBQUMsQ0FBQyxDQUFDO0FBQzFELFFBQUksS0FBSyxFQUFFO0FBQ1QsVUFBSSxJQUFJLEdBQUcsUUFBUSxDQUFDLEtBQUssQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzlCLFVBQUksR0FBRyxHQUFHLEtBQUssQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNuQixhQUFTOztVQUFHLE1BQU0sRUFBRSxTQUFTLEdBQUcsUUFBUSxHQUFHLE9BQU8sQUFBQyxFQUFDLElBQUksRUFBRSxHQUFHLEFBQUM7UUFBRSxJQUFJO09BQUssQ0FBRztLQUM3RTtBQUNELFdBQU8sSUFBSSxDQUFDO0dBQ2IsQ0FBQzs7QUFFRixNQUFJLHdCQUF3QixH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Ozs7QUFHUCxVQUFJLEtBQUssR0FBRyxLQUFLLENBQUMsUUFBUSxDQUFDLElBQUksQ0FBQyxJQUFJLENBQUMsS0FBSyxDQUFDLFFBQVEsQ0FBQyxDQUFDO0FBQ3JELFdBQUssQ0FBQyxLQUFLLENBQUMsU0FBUyxJQUFJLHdDQUF3QyxDQUFDO0FBQ2xFLGFBQU8sS0FBSyxDQUFDOztLQUVkO0dBQ0YsQ0FBQyxDQUFDOztBQUVILFdBQVMsbUJBQW1CLENBQUMsU0FBUyxFQUFFLGFBQWEsRUFBRSxVQUFVLEVBQUUsS0FBSyxFQUFFLFVBQVUsRUFBRSxLQUFLLEVBQUUsZUFBZSxFQUFFO0FBQzVHLFFBQUksWUFBWSxHQUFHLGVBQWUsQ0FBQyxTQUFTLEVBQUUsYUFBYSxFQUFFLFVBQVUsRUFBRSxLQUFLLEVBQUUsVUFBVSxFQUFFLEtBQUssQ0FBQyxDQUFDOztBQUVuRyxRQUFJLFNBQVMsQ0FBQyxHQUFHLENBQUMsTUFBTSxDQUFDLEtBQUssYUFBYSxFQUFFO0FBQzNDLGFBQ0U7QUFBQyxlQUFPOztBQUNOLGNBQUksRUFBRSxVQUFVLENBQUMsR0FBRyxDQUFDLGFBQWEsQ0FBQyxDQUFDLEdBQUcsQ0FBQyxPQUFPLENBQUMsQUFBQztBQUNqRCxlQUFLLEVBQUUsS0FBSyxBQUFDOztRQUNiLFlBQVk7T0FBVyxDQUN6QjtLQUNILE1BQ0k7QUFDSCxhQUFPLFlBQVksQ0FBQztLQUNyQjtHQUNGOztBQUVELElBQUUsQ0FBQyxhQUFhLEdBQUcsYUFBYSxDQUFDO0FBQ2pDLElBQUUsQ0FBQyxtQkFBbUIsR0FBRyxtQkFBbUIsQ0FBQztDQUM5QyxDQUFDLENBQUMiLCJmaWxlIjoiRGF0YXNldFJlY29yZENsYXNzZXMuRGF0YXNldFJlY29yZENsYXNzLmpzIiwic291cmNlc0NvbnRlbnQiOlsiLyogZ2xvYmFsIF8sIFdkaywgd2RrICovXG4vKiBqc2hpbnQgZXNuZXh0OiB0cnVlLCBlcW51bGw6IHRydWUsIC1XMDE0ICovXG5cbi8qKlxuICogVGhpcyBmaWxlIHByb3ZpZGVzIGEgY3VzdG9tIFJlY29yZCBDb21wb25lbnQgd2hpY2ggaXMgdXNlZCBieSB0aGUgbmV3IFdka1xuICogRmx1eCBhcmNoaXRlY3R1cmUuXG4gKlxuICogVGhlIHNpYmxpbmcgZmlsZSBEYXRhc2V0UmVjb3JkQ2xhc3Nlcy5EYXRhc2V0UmVjb3JkQ2xhc3MuanMgaXMgZ2VuZXJhdGVkXG4gKiBmcm9tIHRoaXMgZmlsZSB1c2luZyB0aGUganN4IGNvbXBpbGVyLiBFdmVudHVhbGx5LCB0aGlzIGZpbGUgd2lsbCBiZVxuICogY29tcGlsZWQgZHVyaW5nIGJ1aWxkIHRpbWUtLXRoaXMgaXMgYSBzaG9ydC10ZXJtIHNvbHV0aW9uLlxuICpcbiAqIGB3ZGtgIGlzIHRoZSBsZWdhY3kgZ2xvYmFsIG9iamVjdCwgYW5kIGBXZGtgIGlzIHRoZSBuZXcgZ2xvYmFsIG9iamVjdFxuICovXG5cbndkay5uYW1lc3BhY2UoJ2V1cGF0aGRiLnJlY29yZHMnLCBmdW5jdGlvbihucykge1xuICBcInVzZSBzdHJpY3RcIjtcblxuICB2YXIgUmVhY3QgPSBXZGsuUmVhY3Q7XG5cbiAgLy8gVXNlIEVsZW1lbnQuaW5uZXJUZXh0IHRvIHN0cmlwIFhNTFxuICBmdW5jdGlvbiBzdHJpcFhNTChzdHIpIHtcbiAgICB2YXIgZGl2ID0gZG9jdW1lbnQuY3JlYXRlRWxlbWVudCgnZGl2Jyk7XG4gICAgZGl2LmlubmVySFRNTCA9IHN0cjtcbiAgICByZXR1cm4gZGl2LnRleHRDb250ZW50O1xuICB9XG5cbiAgLy8gZm9ybWF0IGlzIHt0ZXh0fSh7bGlua30pXG4gIHZhciBmb3JtYXRMaW5rID0gZnVuY3Rpb24gZm9ybWF0TGluayhsaW5rLCBvcHRzKSB7XG4gICAgb3B0cyA9IG9wdHMgfHwge307XG4gICAgdmFyIG5ld1dpbmRvdyA9ICEhb3B0cy5uZXdXaW5kb3c7XG4gICAgdmFyIG1hdGNoID0gLyguKilcXCgoLiopXFwpLy5leGVjKGxpbmsucmVwbGFjZSgvXFxuL2csICcgJykpO1xuICAgIGlmIChtYXRjaCkge1xuICAgICAgdmFyIHRleHQgPSBzdHJpcFhNTChtYXRjaFsxXSk7XG4gICAgICB2YXIgdXJsID0gbWF0Y2hbMl07XG4gICAgICByZXR1cm4gKCA8YSB0YXJnZXQ9e25ld1dpbmRvdyA/ICdfYmxhbmsnIDogJ19zZWxmJ30gaHJlZj17dXJsfT57dGV4dH08L2E+ICk7XG4gICAgfVxuICAgIHJldHVybiBudWxsO1xuICB9O1xuXG4gIHZhciByZW5kZXJQcmltYXJ5UHVibGljYXRpb24gPSBmdW5jdGlvbiByZW5kZXJQcmltYXJ5UHVibGljYXRpb24ocHVibGljYXRpb24pIHtcbiAgICB2YXIgcHVibWVkTGluayA9IHB1YmxpY2F0aW9uLmZpbmQoZnVuY3Rpb24ocHViKSB7XG4gICAgICByZXR1cm4gcHViLmdldCgnbmFtZScpID09ICdwdWJtZWRfbGluayc7XG4gICAgfSk7XG4gICAgcmV0dXJuIGZvcm1hdExpbmsocHVibWVkTGluay5nZXQoJ3ZhbHVlJyksIHsgbmV3V2luZG93OiB0cnVlIH0pO1xuICB9O1xuXG4gIHZhciByZW5kZXJQcmltYXJ5Q29udGFjdCA9IGZ1bmN0aW9uIHJlbmRlclByaW1hcnlDb250YWN0KGNvbnRhY3QsIGluc3RpdHV0aW9uKSB7XG4gICAgcmV0dXJuIGNvbnRhY3QgKyAnLCAnICsgaW5zdGl0dXRpb247XG4gIH07XG5cbiAgdmFyIHJlbmRlclNvdXJjZVZlcnNpb24gPSBmdW5jdGlvbih2ZXJzaW9uKSB7XG4gICAgdmFyIG5hbWUgPSB2ZXJzaW9uLmZpbmQodiA9PiB2LmdldCgnbmFtZScpID09PSAndmVyc2lvbicpO1xuICAgIHJldHVybiAoXG4gICAgICBuYW1lLmdldCgndmFsdWUnKSArICcgKFRoZSBkYXRhIHByb3ZpZGVyXFwncyB2ZXJzaW9uIG51bWJlciBvciBwdWJsaWNhdGlvbiBkYXRlLCBmcm9tJyArXG4gICAgICAnIHRoZSBzaXRlIHRoZSBkYXRhIHdhcyBhY3F1aXJlZC4gSW4gdGhlIHJhcmUgY2FzZSBuZWl0aGVyIGlzIGF2YWlsYWJsZSwnICtcbiAgICAgICcgdGhlIGRvd25sb2FkIGRhdGUuKSdcbiAgICApO1xuICB9O1xuXG4gIHZhciBPcmdhbmlzbXMgPSBSZWFjdC5jcmVhdGVDbGFzcyh7XG4gICAgcmVuZGVyKCkge1xuICAgICAgdmFyIHsgb3JnYW5pc21zIH0gPSB0aGlzLnByb3BzO1xuICAgICAgaWYgKCFvcmdhbmlzbXMpIHJldHVybiBudWxsO1xuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGRpdj5cbiAgICAgICAgICA8aDI+T3JnYW5pc21zIHRoaXMgZGF0YSBzZXQgaXMgbWFwcGVkIHRvIGluIFBsYXNtb0RCPC9oMj5cbiAgICAgICAgICA8dWw+e29yZ2FuaXNtcy5zcGxpdCgvLFxccyovKS5tYXAodGhpcy5fcmVuZGVyT3JnYW5pc20pLnRvQXJyYXkoKX08L3VsPlxuICAgICAgICA8L2Rpdj5cbiAgICAgICk7XG4gICAgfSxcblxuICAgIF9yZW5kZXJPcmdhbmlzbShvcmdhbmlzbSwgaW5kZXgpIHtcbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxsaSBrZXk9e2luZGV4fT48aT57b3JnYW5pc219PC9pPjwvbGk+XG4gICAgICApO1xuICAgIH1cbiAgfSk7XG5cbiAgdmFyIFNlYXJjaGVzID0gUmVhY3QuY3JlYXRlQ2xhc3Moe1xuICAgIHJlbmRlcigpIHtcbiAgICAgIHZhciBzZWFyY2hlcyA9IHRoaXMucHJvcHMuc2VhcmNoZXMuZ2V0KCdyb3dzJykuZmlsdGVyKHRoaXMuX3Jvd0lzUXVlc3Rpb24pO1xuXG4gICAgICBpZiAoc2VhcmNoZXMuc2l6ZSA9PT0gMCkgcmV0dXJuIG51bGw7XG5cbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxkaXY+XG4gICAgICAgICAgPGgyPlNlYXJjaCBvciB2aWV3IHRoaXMgZGF0YSBzZXQgaW4gUGxhc21vREI8L2gyPlxuICAgICAgICAgIDx1bD5cbiAgICAgICAgICAgIHtzZWFyY2hlcy5tYXAodGhpcy5fcmVuZGVyU2VhcmNoKS50b0FycmF5KCl9XG4gICAgICAgICAgPC91bD5cbiAgICAgICAgPC9kaXY+XG4gICAgICApO1xuICAgIH0sXG5cbiAgICBfcm93SXNRdWVzdGlvbihyb3cpIHtcbiAgICAgIHZhciB0eXBlID0gcm93LmZpbmQoYXR0ciA9PiBhdHRyLmdldCgnbmFtZScpID09ICd0YXJnZXRfdHlwZScpO1xuICAgICAgcmV0dXJuIHR5cGUgJiYgdHlwZS5nZXQoJ3ZhbHVlJykgPT0gJ3F1ZXN0aW9uJztcbiAgICB9LFxuXG4gICAgX3JlbmRlclNlYXJjaChzZWFyY2gsIGluZGV4KSB7XG4gICAgICB2YXIgbmFtZSA9IHNlYXJjaC5maW5kKGF0dHIgPT4gYXR0ci5nZXQoJ25hbWUnKSA9PSAndGFyZ2V0X25hbWUnKS5nZXQoJ3ZhbHVlJyk7XG4gICAgICB2YXIgcXVlc3Rpb24gPSB0aGlzLnByb3BzLnF1ZXN0aW9ucy5maW5kKHEgPT4gcS5nZXQoJ25hbWUnKSA9PT0gbmFtZSk7XG5cbiAgICAgIGlmIChxdWVzdGlvbiA9PSBudWxsKSByZXR1cm4gbnVsbDtcblxuICAgICAgdmFyIHJlY29yZENsYXNzID0gdGhpcy5wcm9wcy5yZWNvcmRDbGFzc2VzLmZpbmQociA9PiByLmdldCgnZnVsbE5hbWUnKSA9PT0gcXVlc3Rpb24uZ2V0KCdjbGFzcycpKTtcbiAgICAgIHZhciBzZWFyY2hOYW1lID0gYElkZW50aWZ5ICR7cmVjb3JkQ2xhc3MuZ2V0KCdkaXNwbGF5TmFtZVBsdXJhbCcpfSBieSAke3F1ZXN0aW9uLmdldCgnZGlzcGxheU5hbWUnKX1gO1xuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGxpIGtleT17aW5kZXh9PlxuICAgICAgICAgIDxhIGhyZWY9eycvYS9zaG93UXVlc3Rpb24uZG8/cXVlc3Rpb25GdWxsTmFtZT0nICsgbmFtZX0+e3NlYXJjaE5hbWV9PC9hPlxuICAgICAgICA8L2xpPlxuICAgICAgKTtcbiAgICB9XG4gIH0pO1xuXG4gIHZhciBMaW5rcyA9IFJlYWN0LmNyZWF0ZUNsYXNzKHtcbiAgICByZW5kZXIoKSB7XG4gICAgICB2YXIgeyBsaW5rcyB9ID0gdGhpcy5wcm9wcztcblxuICAgICAgaWYgKGxpbmtzLmdldCgncm93cycpLnNpemUgPT09IDApIHJldHVybiBudWxsO1xuXG4gICAgICByZXR1cm4gKFxuICAgICAgICA8ZGl2PlxuICAgICAgICAgIDxoMj5MaW5rczwvaDI+XG4gICAgICAgICAgPHVsPiB7bGlua3MuZ2V0KCdyb3dzJykubWFwKHRoaXMuX3JlbmRlckxpbmspLnRvQXJyYXkoKX0gPC91bD5cbiAgICAgICAgPC9kaXY+XG4gICAgICApO1xuICAgIH0sXG5cbiAgICBfcmVuZGVyTGluayhsaW5rLCBpbmRleCkge1xuICAgICAgdmFyIGh5cGVyTGluayA9IGxpbmsuZmluZChhdHRyID0+IGF0dHIuZ2V0KCduYW1lJykgPT0gJ2h5cGVyX2xpbmsnKTtcbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxsaSBrZXk9e2luZGV4fT57Zm9ybWF0TGluayhoeXBlckxpbmsuZ2V0KCd2YWx1ZScpKX08L2xpPlxuICAgICAgKTtcbiAgICB9XG4gIH0pO1xuXG4gIHZhciBDb250YWN0cyA9IFJlYWN0LmNyZWF0ZUNsYXNzKHtcbiAgICByZW5kZXIoKSB7XG4gICAgICB2YXIgeyBjb250YWN0cyB9ID0gdGhpcy5wcm9wcztcbiAgICAgIGlmIChjb250YWN0cy5nZXQoJ3Jvd3MnKS5zaXplID09PSAwKSByZXR1cm4gbnVsbDtcbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxkaXY+XG4gICAgICAgICAgPGg0PkNvbnRhY3RzPC9oND5cbiAgICAgICAgICA8dWw+XG4gICAgICAgICAgICB7Y29udGFjdHMuZ2V0KCdyb3dzJykubWFwKHRoaXMuX3JlbmRlckNvbnRhY3QpLnRvQXJyYXkoKX1cbiAgICAgICAgICA8L3VsPlxuICAgICAgICA8L2Rpdj5cbiAgICAgICk7XG4gICAgfSxcblxuICAgIF9yZW5kZXJDb250YWN0KGNvbnRhY3QsIGluZGV4KSB7XG4gICAgICB2YXIgY29udGFjdF9uYW1lID0gY29udGFjdC5maW5kKGMgPT4gYy5nZXQoJ25hbWUnKSA9PSAnY29udGFjdF9uYW1lJyk7XG4gICAgICB2YXIgYWZmaWxpYXRpb24gPSBjb250YWN0LmZpbmQoYyA9PiBjLmdldCgnbmFtZScpID09ICdhZmZpbGlhdGlvbicpO1xuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGxpIGtleT17aW5kZXh9Pntjb250YWN0X25hbWUuZ2V0KCd2YWx1ZScpfSwge2FmZmlsaWF0aW9uLmdldCgndmFsdWUnKX08L2xpPlxuICAgICAgKTtcbiAgICB9XG4gIH0pO1xuXG4gIHZhciBQdWJsaWNhdGlvbnMgPSBSZWFjdC5jcmVhdGVDbGFzcyh7XG4gICAgcmVuZGVyKCkge1xuICAgICAgdmFyIHsgcHVibGljYXRpb25zIH0gPSB0aGlzLnByb3BzO1xuICAgICAgdmFyIHJvd3MgPSBwdWJsaWNhdGlvbnMuZ2V0KCdyb3dzJyk7XG4gICAgICBpZiAocm93cy5zaXplID09PSAwKSByZXR1cm4gbnVsbDtcbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxkaXY+XG4gICAgICAgICAgPGg0PlB1YmxpY2F0aW9uczwvaDQ+XG4gICAgICAgICAgPHVsPntyb3dzLm1hcCh0aGlzLl9yZW5kZXJQdWJsaWNhdGlvbikudG9BcnJheSgpfTwvdWw+XG4gICAgICAgIDwvZGl2PlxuICAgICAgKTtcbiAgICB9LFxuXG4gICAgX3JlbmRlclB1YmxpY2F0aW9uKHB1YmxpY2F0aW9uLCBpbmRleCkge1xuICAgICAgdmFyIHB1Ym1lZF9saW5rID0gcHVibGljYXRpb24uZmluZChwID0+IHAuZ2V0KCduYW1lJykgPT0gJ3B1Ym1lZF9saW5rJyk7XG4gICAgICByZXR1cm4gKFxuICAgICAgICA8bGkga2V5PXtpbmRleH0+e2Zvcm1hdExpbmsocHVibWVkX2xpbmsuZ2V0KCd2YWx1ZScpKX08L2xpPlxuICAgICAgKTtcbiAgICB9XG4gIH0pO1xuXG4gIHZhciBDb250YWN0c0FuZFB1YmxpY2F0aW9ucyA9IFJlYWN0LmNyZWF0ZUNsYXNzKHtcbiAgICByZW5kZXIoKSB7XG4gICAgICB2YXIgeyBjb250YWN0cywgcHVibGljYXRpb25zIH0gPSB0aGlzLnByb3BzO1xuXG4gICAgICBpZiAoY29udGFjdHMuZ2V0KCdyb3dzJykuc2l6ZSA9PT0gMCAmJiBwdWJsaWNhdGlvbnMuZ2V0KCdyb3dzJykuc2l6ZSA9PT0gMCkgcmV0dXJuIG51bGw7XG5cbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxkaXY+XG4gICAgICAgICAgPGgyPkFkZGl0aW9uYWwgQ29udGFjdHMgYW5kIFB1YmxpY2F0aW9uczwvaDI+XG4gICAgICAgICAgPENvbnRhY3RzIGNvbnRhY3RzPXtjb250YWN0c30vPlxuICAgICAgICAgIDxQdWJsaWNhdGlvbnMgcHVibGljYXRpb25zPXtwdWJsaWNhdGlvbnN9Lz5cbiAgICAgICAgPC9kaXY+XG4gICAgICApO1xuICAgIH1cbiAgfSk7XG5cbiAgdmFyIFJlbGVhc2VIaXN0b3J5ID0gUmVhY3QuY3JlYXRlQ2xhc3Moe1xuICAgIHJlbmRlcigpIHtcbiAgICAgIHZhciB7IGhpc3RvcnkgfSA9IHRoaXMucHJvcHM7XG4gICAgICBpZiAoaGlzdG9yeS5nZXQoJ3Jvd3MnKS5zaXplID09PSAwKSByZXR1cm4gbnVsbDtcbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxkaXY+XG4gICAgICAgICAgPGgyPkRhdGEgU2V0IFJlbGVhc2UgSGlzdG9yeTwvaDI+XG4gICAgICAgICAgPHRhYmxlPlxuICAgICAgICAgICAgPHRoZWFkPlxuICAgICAgICAgICAgICA8dHI+XG4gICAgICAgICAgICAgICAgPHRoPkV1UGF0aERCIFJlbGVhc2U8L3RoPlxuICAgICAgICAgICAgICAgIDx0aD5HZW5vbWUgU291cmNlPC90aD5cbiAgICAgICAgICAgICAgICA8dGg+QW5ub3RhdGlvbiBTb3VyY2U8L3RoPlxuICAgICAgICAgICAgICAgIDx0aD5Ob3RlczwvdGg+XG4gICAgICAgICAgICAgIDwvdHI+XG4gICAgICAgICAgICA8L3RoZWFkPlxuICAgICAgICAgICAgPHRib2R5PlxuICAgICAgICAgICAgICB7aGlzdG9yeS5nZXQoJ3Jvd3MnKS5tYXAodGhpcy5fcmVuZGVyUm93KS50b0FycmF5KCl9XG4gICAgICAgICAgICA8L3Rib2R5PlxuICAgICAgICAgIDwvdGFibGU+XG4gICAgICAgIDwvZGl2PlxuICAgICAgKTtcbiAgICB9LFxuXG4gICAgX3JlbmRlclJvdyhhdHRyaWJ1dGVzKSB7XG4gICAgICB2YXIgYXR0cnMgPSBfLmluZGV4QnkoYXR0cmlidXRlcy50b0pTKCksICduYW1lJyk7XG5cbiAgICAgIHZhciByZWxlYXNlRGF0ZSA9IGF0dHJzLnJlbGVhc2VfZGF0ZS52YWx1ZS5zcGxpdCgvXFxzKy8pWzBdO1xuXG4gICAgICB2YXIgcmVsZWFzZSA9IGF0dHJzLmJ1aWxkLnZhbHVlID09IDBcbiAgICAgICAgPyAnSW5pdGlhbCByZWxlYXNlJ1xuICAgICAgICA6IGAke2F0dHJzLnByb2plY3QudmFsdWV9ICR7YXR0cnMucmVsZWFzZV9udW1iZXIudmFsdWV9ICR7cmVsZWFzZURhdGV9YDtcblxuICAgICAgdmFyIGdlbm9tZVNvdXJjZSA9IGF0dHJzLmdlbm9tZV9zb3VyY2UudmFsdWVcbiAgICAgICAgPyBhdHRycy5nZW5vbWVfc291cmNlLnZhbHVlICsgJyAoJyArIGF0dHJzLmdlbm9tZV92ZXJzaW9uLnZhbHVlICsgJyknXG4gICAgICAgIDogJyc7XG5cbiAgICAgIHZhciBhbm5vdGF0aW9uU291cmNlID0gYXR0cnMuYW5ub3RhdGlvbl9zb3VyY2UudmFsdWVcbiAgICAgICAgPyBhdHRycy5hbm5vdGF0aW9uX3NvdXJjZS52YWx1ZSArICcgKCcgKyBhdHRycy5hbm5vdGF0aW9uX3ZlcnNpb24udmFsdWUgKyAnKSdcbiAgICAgICAgOiAnJztcblxuICAgICAgcmV0dXJuIChcbiAgICAgICAgPHRyPlxuICAgICAgICAgIDx0ZD57cmVsZWFzZX08L3RkPlxuICAgICAgICAgIDx0ZD57Z2Vub21lU291cmNlfTwvdGQ+XG4gICAgICAgICAgPHRkPnthbm5vdGF0aW9uU291cmNlfTwvdGQ+XG4gICAgICAgICAgPHRkPnthdHRycy5ub3RlLnZhbHVlfTwvdGQ+XG4gICAgICAgIDwvdHI+XG4gICAgICApO1xuICAgIH1cbiAgfSk7XG5cbiAgdmFyIFZlcnNpb25zID0gUmVhY3QuY3JlYXRlQ2xhc3Moe1xuICAgIHJlbmRlcigpIHtcbiAgICAgIHZhciB7IHZlcnNpb25zIH0gPSB0aGlzLnByb3BzO1xuICAgICAgdmFyIHJvd3MgPSB2ZXJzaW9ucy5nZXQoJ3Jvd3MnKTtcblxuICAgICAgaWYgKHJvd3Muc2l6ZSA9PT0gMCkgcmV0dXJuIG51bGw7XG5cbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxkaXY+XG4gICAgICAgICAgPGgyPlByb3ZpZGVyJ3MgVmVyc2lvbjwvaDI+XG4gICAgICAgICAgPHA+XG4gICAgICAgICAgICBUaGUgZGF0YSBzZXQgdmVyc2lvbiBzaG93biBoZXJlIGlzIHRoZSBkYXRhIHByb3ZpZGVyJ3MgdmVyc2lvblxuICAgICAgICAgICAgbnVtYmVyIG9yIHB1YmxpY2F0aW9uIGRhdGUgaW5kaWNhdGVkIG9uIHRoZSBzaXRlIGZyb20gd2hpY2ggd2VcbiAgICAgICAgICAgIGRvd25sb2FkZWQgdGhlIGRhdGEuIEluIHRoZSByYXJlIGNhc2UgdGhhdCB0aGVzZSBhcmUgbm90IGF2YWlsYWJsZSxcbiAgICAgICAgICAgIHRoZSB2ZXJzaW9uIGlzIHRoZSBkYXRlIHRoYXQgdGhlIGRhdGEgc2V0IHdhcyBkb3dubG9hZGVkLlxuICAgICAgICAgIDwvcD5cbiAgICAgICAgICA8dGFibGU+XG4gICAgICAgICAgICA8dGhlYWQ+XG4gICAgICAgICAgICAgIDx0cj5cbiAgICAgICAgICAgICAgICA8dGg+T3JnYW5pc208L3RoPlxuICAgICAgICAgICAgICAgIDx0aD5Qcm92aWRlcidzIFZlcnNpb248L3RoPlxuICAgICAgICAgICAgICA8L3RyPlxuICAgICAgICAgICAgPC90aGVhZD5cbiAgICAgICAgICAgIDx0Ym9keT5cbiAgICAgICAgICAgICAge3Jvd3MubWFwKHRoaXMuX3JlbmRlclJvdykudG9BcnJheSgpfVxuICAgICAgICAgICAgPC90Ym9keT5cbiAgICAgICAgICA8L3RhYmxlPlxuICAgICAgICA8L2Rpdj5cbiAgICAgICk7XG4gICAgfSxcblxuICAgIF9yZW5kZXJSb3coYXR0cmlidXRlcykge1xuICAgICAgdmFyIGF0dHJzID0gXy5pbmRleEJ5KGF0dHJpYnV0ZXMudG9KUygpLCAnbmFtZScpO1xuICAgICAgcmV0dXJuIChcbiAgICAgICAgPHRyPlxuICAgICAgICAgIDx0ZD57YXR0cnMub3JnYW5pc20udmFsdWV9PC90ZD5cbiAgICAgICAgICA8dGQ+e2F0dHJzLnZlcnNpb24udmFsdWV9PC90ZD5cbiAgICAgICAgPC90cj5cbiAgICAgICk7XG4gICAgfVxuICB9KTtcblxuICB2YXIgR3JhcGhzID0gUmVhY3QuY3JlYXRlQ2xhc3Moe1xuICAgIHJlbmRlcigpIHtcbiAgICAgIHZhciB7IGdyYXBocyB9ID0gdGhpcy5wcm9wcztcbiAgICAgIHZhciByb3dzID0gZ3JhcGhzLmdldCgncm93cycpO1xuICAgICAgaWYgKHJvd3Muc2l6ZSA9PT0gMCkgcmV0dXJuIG51bGw7XG4gICAgICByZXR1cm4gKFxuICAgICAgICA8ZGl2PlxuICAgICAgICAgIDxoMj5FeGFtcGxlIEdyYXBoczwvaDI+XG4gICAgICAgICAgPHVsPntyb3dzLm1hcCh0aGlzLl9yZW5kZXJHcmFwaCkudG9BcnJheSgpfTwvdWw+XG4gICAgICAgIDwvZGl2PlxuICAgICAgKTtcbiAgICB9LFxuXG4gICAgX3JlbmRlckdyYXBoKGdyYXBoLCBpbmRleCkge1xuICAgICAgdmFyIGcgPSBfLmluZGV4QnkoZ3JhcGgudG9KUygpLCAnbmFtZScpO1xuXG4gICAgICB2YXIgZGlzcGxheU5hbWUgPSBnLmRpc3BsYXlfbmFtZS52YWx1ZTtcblxuICAgICAgdmFyIGJhc2VVcmwgPSAnL2NnaS1iaW4vZGF0YVBsb3R0ZXIucGwnICtcbiAgICAgICAgJz90eXBlPScgKyBnLm1vZHVsZS52YWx1ZSArXG4gICAgICAgICcmcHJvamVjdF9pZD0nICsgZy5wcm9qZWN0X2lkLnZhbHVlICtcbiAgICAgICAgJyZkYXRhc2V0PScgKyBnLmRhdGFzZXRfbmFtZS52YWx1ZSArXG4gICAgICAgICcmdGVtcGxhdGU9JyArIChnLmlzX2dyYXBoX2N1c3RvbS52YWx1ZSA9PT0gJ2ZhbHNlJyA/IDEgOiAnJykgK1xuICAgICAgICAnJmlkPScgKyBnLmdyYXBoX2lkcy52YWx1ZTtcblxuICAgICAgdmFyIGltZ1VybCA9IGJhc2VVcmwgKyAnJmZtdD1wbmcnO1xuICAgICAgdmFyIHRhYmxlVXJsID0gYmFzZVVybCArICcmZm10PXRhYmxlJztcblxuICAgICAgcmV0dXJuIChcbiAgICAgICAgPGxpIGtleT17aW5kZXh9PlxuICAgICAgICAgIDxoMz57ZGlzcGxheU5hbWV9PC9oMz5cbiAgICAgICAgICA8ZGl2IGNsYXNzTmFtZT1cImV1cGF0aGRiLURhdGFzZXRSZWNvcmQtR3JhcGhNZXRhXCI+XG4gICAgICAgICAgICA8aDM+RGVzY3JpcHRpb248L2gzPlxuICAgICAgICAgICAgPHAgZGFuZ2Vyb3VzbHlTZXRJbm5lckhUTUw9e3tfX2h0bWw6IGcuZGVzY3JpcHRpb24udmFsdWV9fS8+XG4gICAgICAgICAgICA8aDM+WC1heGlzPC9oMz5cbiAgICAgICAgICAgIDxwPntnLnhfYXhpcy52YWx1ZX08L3A+XG4gICAgICAgICAgICA8aDM+WS1heGlzPC9oMz5cbiAgICAgICAgICAgIDxwPntnLnlfYXhpcy52YWx1ZX08L3A+XG4gICAgICAgICAgPC9kaXY+XG4gICAgICAgICAgPGRpdiBjbGFzc05hbWU9XCJldXBhdGhkYi1EYXRhc2V0UmVjb3JkLUdyYXBoRGF0YVwiPlxuICAgICAgICAgICAgPGltZyBjbGFzc05hbWU9XCJldXBhdGhkYi1EYXRhc2V0UmVjb3JkLUdyYXBoSW1nXCIgc3JjPXtpbWdVcmx9Lz5cbiAgICAgICAgICA8L2Rpdj5cbiAgICAgICAgPC9saT5cbiAgICAgICk7XG4gICAgfVxuICB9KTtcblxuICB2YXIgRGF0YXNldFJlY29yZCA9IFJlYWN0LmNyZWF0ZUNsYXNzKHtcbiAgICByZW5kZXIoKSB7XG4gICAgICB2YXIgeyByZWNvcmQsIHF1ZXN0aW9ucywgcmVjb3JkQ2xhc3NlcyB9ID0gdGhpcy5wcm9wcztcbiAgICAgIHZhciBhdHRyaWJ1dGVzID0gcmVjb3JkLmdldCgnYXR0cmlidXRlcycpO1xuICAgICAgdmFyIHRhYmxlcyA9IHJlY29yZC5nZXQoJ3RhYmxlcycpO1xuICAgICAgdmFyIHRpdGxlQ2xhc3MgPSAnZXVwYXRoZGItRGF0YXNldFJlY29yZC10aXRsZSc7XG5cbiAgICAgIHZhciBpZCA9IHJlY29yZC5nZXQoJ2lkJyk7XG4gICAgICB2YXIgc3VtbWFyeSA9IGF0dHJpYnV0ZXMuZ2V0SW4oWydzdW1tYXJ5JywgJ3ZhbHVlJ10pO1xuICAgICAgdmFyIHJlbGVhc2VJbmZvID0gYXR0cmlidXRlcy5nZXRJbihbJ2V1cGF0aF9yZWxlYXNlJywgJ3ZhbHVlJ10pO1xuICAgICAgdmFyIHByaW1hcnlQdWJsaWNhdGlvbiA9IHRhYmxlcy5nZXRJbihbJ1B1YmxpY2F0aW9ucycsICdyb3dzJywgMF0pO1xuICAgICAgdmFyIGNvbnRhY3QgPSBhdHRyaWJ1dGVzLmdldEluKFsnY29udGFjdCcsICd2YWx1ZSddKTtcbiAgICAgIHZhciBpbnN0aXR1dGlvbiA9IGF0dHJpYnV0ZXMuZ2V0SW4oWydpbnN0aXR1dGlvbicsICd2YWx1ZSddKTtcbiAgICAgIHZhciB2ZXJzaW9uID0gdGFibGVzLmdldEluKFsnVmVyc2lvbicsICdyb3dzJywgMF0pO1xuICAgICAgdmFyIG9yZ2FuaXNtID0gYXR0cmlidXRlcy5nZXRJbihbJ29yZ2FuaXNtX3ByZWZpeCcsICd2YWx1ZSddKTtcbiAgICAgIHZhciBvcmdhbmlzbXMgPSBhdHRyaWJ1dGVzLmdldEluKFsnb3JnYW5pc21zJywgJ3ZhbHVlJ10pO1xuICAgICAgdmFyIFJlZmVyZW5jZXMgPSB0YWJsZXMuZ2V0KCdSZWZlcmVuY2VzJyk7XG4gICAgICB2YXIgSHlwZXJMaW5rcyA9IHRhYmxlcy5nZXQoJ0h5cGVyTGlua3MnKTtcbiAgICAgIHZhciBDb250YWN0cyA9IHRhYmxlcy5nZXQoJ0NvbnRhY3RzJyk7XG4gICAgICB2YXIgUHVibGljYXRpb25zID0gdGFibGVzLmdldCgnUHVibGljYXRpb25zJyk7XG4gICAgICB2YXIgZGVzY3JpcHRpb24gPSBhdHRyaWJ1dGVzLmdldEluKFsnZGVzY3JpcHRpb24nLCAndmFsdWUnXSk7XG4gICAgICB2YXIgR2Vub21lSGlzdG9yeSA9IHRhYmxlcy5nZXQoJ0dlbm9tZUhpc3RvcnknKTtcbiAgICAgIHZhciBWZXJzaW9uID0gdGFibGVzLmdldCgnVmVyc2lvbicpO1xuICAgICAgdmFyIEV4YW1wbGVHcmFwaHMgPSB0YWJsZXMuZ2V0KCdFeGFtcGxlR3JhcGhzJyk7XG5cbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxkaXYgY2xhc3NOYW1lPVwiZXVwYXRoZGItRGF0YXNldFJlY29yZCB1aS1oZWxwZXItY2xlYXJmaXhcIj5cbiAgICAgICAgICA8aDEgZGFuZ2Vyb3VzbHlTZXRJbm5lckhUTUw9e3tcbiAgICAgICAgICAgIF9faHRtbDogJ0RhdGEgU2V0OiA8c3BhbiBjbGFzcz1cIicgKyB0aXRsZUNsYXNzICsgJ1wiPicgKyBpZCArICc8L3NwYW4+J1xuICAgICAgICAgIH19Lz5cblxuICAgICAgICAgIDxkaXYgY2xhc3NOYW1lPVwiZXVwYXRoZGItRGF0YXNldFJlY29yZC1Db250YWluZXIgdWktaGVscGVyLWNsZWFyZml4XCI+XG5cbiAgICAgICAgICAgIDxoci8+XG5cbiAgICAgICAgICAgIDx0YWJsZSBjbGFzc05hbWU9XCJldXBhdGhkYi1EYXRhc2V0UmVjb3JkLWhlYWRlclRhYmxlXCI+XG4gICAgICAgICAgICAgIDx0Ym9keT5cblxuICAgICAgICAgICAgICAgIDx0cj5cbiAgICAgICAgICAgICAgICAgIDx0aD5TdW1tYXJ5OjwvdGg+XG4gICAgICAgICAgICAgICAgICA8dGQgZGFuZ2Vyb3VzbHlTZXRJbm5lckhUTUw9e3tfX2h0bWw6IHN1bW1hcnl9fS8+XG4gICAgICAgICAgICAgICAgPC90cj5cblxuICAgICAgICAgICAgICAgIHtvcmdhbmlzbSA/IChcbiAgICAgICAgICAgICAgICAgIDx0cj5cbiAgICAgICAgICAgICAgICAgICAgPHRoPk9yZ2FuaXNtIChzb3VyY2Ugb3IgcmVmZXJlbmNlKTo8L3RoPlxuICAgICAgICAgICAgICAgICAgICA8dGQgZGFuZ2Vyb3VzbHlTZXRJbm5lckhUTUw9e3tfX2h0bWw6IG9yZ2FuaXNtfX0vPlxuICAgICAgICAgICAgICAgICAgPC90cj5cbiAgICAgICAgICAgICAgICApIDogbnVsbH1cblxuICAgICAgICAgICAgICAgIHtwcmltYXJ5UHVibGljYXRpb24gPyAoXG4gICAgICAgICAgICAgICAgICA8dHI+XG4gICAgICAgICAgICAgICAgICAgIDx0aD5QcmltYXJ5IHB1YmxpY2F0aW9uOjwvdGg+XG4gICAgICAgICAgICAgICAgICAgIDx0ZD57cmVuZGVyUHJpbWFyeVB1YmxpY2F0aW9uKHByaW1hcnlQdWJsaWNhdGlvbil9PC90ZD5cbiAgICAgICAgICAgICAgICAgIDwvdHI+XG4gICAgICAgICAgICAgICAgKSA6IG51bGx9XG5cbiAgICAgICAgICAgICAgICB7Y29udGFjdCAmJiBpbnN0aXR1dGlvbiA/IChcbiAgICAgICAgICAgICAgICAgIDx0cj5cbiAgICAgICAgICAgICAgICAgICAgPHRoPlByaW1hcnkgY29udGFjdDo8L3RoPlxuICAgICAgICAgICAgICAgICAgICA8dGQ+e3JlbmRlclByaW1hcnlDb250YWN0KGNvbnRhY3QsIGluc3RpdHV0aW9uKX08L3RkPlxuICAgICAgICAgICAgICAgICAgPC90cj5cbiAgICAgICAgICAgICAgICApIDogbnVsbH1cblxuICAgICAgICAgICAgICAgIHt2ZXJzaW9uID8gKFxuICAgICAgICAgICAgICAgICAgPHRyPlxuICAgICAgICAgICAgICAgICAgICA8dGg+U291cmNlIHZlcnNpb246PC90aD5cbiAgICAgICAgICAgICAgICAgICAgPHRkPntyZW5kZXJTb3VyY2VWZXJzaW9uKHZlcnNpb24pfTwvdGQ+XG4gICAgICAgICAgICAgICAgICA8L3RyPlxuICAgICAgICAgICAgICAgICkgOiBudWxsfVxuXG4gICAgICAgICAgICAgICAge3JlbGVhc2VJbmZvID8gKFxuICAgICAgICAgICAgICAgICAgPHRyPlxuICAgICAgICAgICAgICAgICAgICA8dGg+RXVQYXRoREIgcmVsZWFzZSAjIC8gZGF0ZTo8L3RoPlxuICAgICAgICAgICAgICAgICAgICA8dGQ+e3JlbGVhc2VJbmZvfTwvdGQ+XG4gICAgICAgICAgICAgICAgICA8L3RyPlxuICAgICAgICAgICAgICAgICkgOiBudWxsfVxuXG4gICAgICAgICAgICAgIDwvdGJvZHk+XG4gICAgICAgICAgICA8L3RhYmxlPlxuXG4gICAgICAgICAgICA8aHIvPlxuXG4gICAgICAgICAgICA8ZGl2IGNsYXNzTmFtZT1cImV1cGF0aGRiLURhdGFzZXRSZWNvcmQtTWFpblwiPlxuICAgICAgICAgICAgICA8aDI+RGV0YWlsZWQgRGVzY3JpcHRpb248L2gyPlxuICAgICAgICAgICAgICA8ZGl2IGRhbmdlcm91c2x5U2V0SW5uZXJIVE1MPXt7X19odG1sOiBkZXNjcmlwdGlvbn19Lz5cbiAgICAgICAgICAgICAgPENvbnRhY3RzQW5kUHVibGljYXRpb25zIGNvbnRhY3RzPXtDb250YWN0c30gcHVibGljYXRpb25zPXtQdWJsaWNhdGlvbnN9Lz5cbiAgICAgICAgICAgIDwvZGl2PlxuXG4gICAgICAgICAgICA8ZGl2IGNsYXNzTmFtZT1cImV1cGF0aGRiLURhdGFzZXRSZWNvcmQtU2lkZWJhclwiPlxuICAgICAgICAgICAgICA8T3JnYW5pc21zIG9yZ2FuaXNtcz17b3JnYW5pc21zfS8+XG4gICAgICAgICAgICAgIDxTZWFyY2hlcyBzZWFyY2hlcz17UmVmZXJlbmNlc30gcXVlc3Rpb25zPXtxdWVzdGlvbnN9IHJlY29yZENsYXNzZXM9e3JlY29yZENsYXNzZXN9Lz5cbiAgICAgICAgICAgICAgPExpbmtzIGxpbmtzPXtIeXBlckxpbmtzfS8+XG4gICAgICAgICAgICAgIDxSZWxlYXNlSGlzdG9yeSBoaXN0b3J5PXtHZW5vbWVIaXN0b3J5fS8+XG4gICAgICAgICAgICAgIDxWZXJzaW9ucyB2ZXJzaW9ucz17VmVyc2lvbn0vPlxuICAgICAgICAgICAgPC9kaXY+XG5cbiAgICAgICAgICA8L2Rpdj5cbiAgICAgICAgICA8R3JhcGhzIGdyYXBocz17RXhhbXBsZUdyYXBoc30vPlxuICAgICAgICA8L2Rpdj5cbiAgICAgICk7XG4gICAgfVxuICB9KTtcblxuICB2YXIgVG9vbHRpcCA9IFJlYWN0LmNyZWF0ZUNsYXNzKHtcbiAgICBjb21wb25lbnREaWRNb3VudCgpIHtcbiAgICAgIHRoaXMuX3NldHVwVG9vbHRpcCgpO1xuICAgIH0sXG4gICAgY29tcG9uZW50RGlkVXBkYXRlKCkge1xuICAgICAgdGhpcy5fZGVzdHJveVRvb2x0aXAoKTtcbiAgICAgIHRoaXMuX3NldHVwVG9vbHRpcCgpO1xuICAgIH0sXG4gICAgY29tcG9uZW50V2lsbFVubW91bnQoKSB7XG4gICAgICB0aGlzLl9kZXN0cm95VG9vbHRpcCgpO1xuICAgIH0sXG4gICAgX3NldHVwVG9vbHRpcCgpIHtcbiAgICAgIGlmICh0aGlzLnByb3BzLnRleHQgPT0gbnVsbCkgcmV0dXJuO1xuXG4gICAgICB2YXIgdGV4dCA9IGA8ZGl2IHN0eWxlPVwibWF4LWhlaWdodDogMjAwcHg7IG92ZXJmbG93LXk6IGF1dG87IHBhZGRpbmc6IDJweDtcIj4ke3RoaXMucHJvcHMudGV4dH08L2Rpdj5gO1xuICAgICAgdmFyIHdpZHRoID0gdGhpcy5wcm9wcy53aWR0aDtcblxuICAgICAgdGhpcy4kdGFyZ2V0ID0gJCh0aGlzLmdldERPTU5vZGUoKSkuZmluZCgnLndkay1SZWNvcmRUYWJsZS1yZWNvcmRMaW5rJylcbiAgICAgICAgLndka1Rvb2x0aXAoe1xuICAgICAgICAgIG92ZXJ3cml0ZTogdHJ1ZSxcbiAgICAgICAgICBjb250ZW50OiB7IHRleHQgfSxcbiAgICAgICAgICBzaG93OiB7IGRlbGF5OiAxMDAwIH0sXG4gICAgICAgICAgcG9zaXRpb246IHsgbXk6ICd0b3AgbGVmdCcsIGF0OiAnYm90dG9tIGxlZnQnLCBhZGp1c3Q6IHsgeTogMTIgfSB9XG4gICAgICAgIH0pO1xuICAgIH0sXG4gICAgX2Rlc3Ryb3lUb29sdGlwKCkge1xuICAgICAgLy8gaWYgX3NldHVwVG9vbHRpcCBkb2Vzbid0IGRvIGFueXRoaW5nLCB0aGlzIGlzIGEgbm9vcFxuICAgICAgaWYgKHRoaXMuJHRhcmdldCkge1xuICAgICAgICB0aGlzLiR0YXJnZXQucXRpcCgnZGVzdHJveScsIHRydWUpO1xuICAgICAgfVxuICAgIH0sXG4gICAgcmVuZGVyKCkge1xuICAgICAgLy8gRklYTUUgLSBGaWd1cmUgb3V0IHdoeSB3ZSBsb3NlIHRoZSBmaXhlZC1kYXRhLXRhYmxlIGNsYXNzTmFtZVxuICAgICAgLy8gTG9zaW5nIHRoZSBmaXhlZC1kYXRhLXRhYmxlIGNsYXNzTmFtZSBmb3Igc29tZSByZWFzb24uLi4gYWRkaW5nIGl0IGJhY2suXG4gICAgICB2YXIgY2hpbGQgPSBSZWFjdC5DaGlsZHJlbi5vbmx5KHRoaXMucHJvcHMuY2hpbGRyZW4pO1xuICAgICAgY2hpbGQucHJvcHMuY2xhc3NOYW1lICs9IFwiIHB1YmxpY19maXhlZERhdGFUYWJsZUNlbGxfY2VsbENvbnRlbnRcIjtcbiAgICAgIHJldHVybiBjaGlsZDtcbiAgICAgIC8vcmV0dXJuIHRoaXMucHJvcHMuY2hpbGRyZW47XG4gICAgfVxuICB9KTtcblxuICBmdW5jdGlvbiBkYXRhc2V0Q2VsbFJlbmRlcmVyKGF0dHJpYnV0ZSwgYXR0cmlidXRlTmFtZSwgYXR0cmlidXRlcywgaW5kZXgsIGNvbHVtbkRhdGEsIHdpZHRoLCBkZWZhdWx0UmVuZGVyZXIpIHtcbiAgICB2YXIgcmVhY3RFbGVtZW50ID0gZGVmYXVsdFJlbmRlcmVyKGF0dHJpYnV0ZSwgYXR0cmlidXRlTmFtZSwgYXR0cmlidXRlcywgaW5kZXgsIGNvbHVtbkRhdGEsIHdpZHRoKTtcblxuICAgIGlmIChhdHRyaWJ1dGUuZ2V0KCduYW1lJykgPT09ICdwcmltYXJ5X2tleScpIHtcbiAgICAgIHJldHVybiAoXG4gICAgICAgIDxUb29sdGlwXG4gICAgICAgICAgdGV4dD17YXR0cmlidXRlcy5nZXQoJ2Rlc2NyaXB0aW9uJykuZ2V0KCd2YWx1ZScpfVxuICAgICAgICAgIHdpZHRoPXt3aWR0aH1cbiAgICAgICAgPntyZWFjdEVsZW1lbnR9PC9Ub29sdGlwPlxuICAgICAgKTtcbiAgICB9XG4gICAgZWxzZSB7XG4gICAgICByZXR1cm4gcmVhY3RFbGVtZW50O1xuICAgIH1cbiAgfVxuXG4gIG5zLkRhdGFzZXRSZWNvcmQgPSBEYXRhc2V0UmVjb3JkO1xuICBucy5kYXRhc2V0Q2VsbFJlbmRlcmVyID0gZGF0YXNldENlbGxSZW5kZXJlcjtcbn0pO1xuIl19