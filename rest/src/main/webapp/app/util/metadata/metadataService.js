// MetadataService
tsApp
  .service(
    'metadataService',
    [
      '$http',
      '$q',
      'gpService',
      'utilService',
      function($http, $q, gpService, utilService) {
        console.debug("configure metadataService");

        // The metadata for current terminology
        var metadata = {
          terminology : null,
          precedenceList : null,
          entries : null,
          relationshipTypes : null,
          attributeNames : null,
          termTypes : null,
          generalEntries : null,
          labelSets : null,
          generalEntries : null,
          atomsLabel : "Atoms",
          hierarchiesLabel : "Hierarchies",
          attributesLabel : "Attributes",
          definitionsLabel : "Definitions",
          subsetsLabel : "Subsets",
          relationshipsLabel : "Relationships",
          atomRelationshipsLabel : "Relationships",
          extensionsLabel : "Extensions",
          treeSortField : "nodeName",
          terminologies : null
        };

        // Obtain the data model
        this.getModel = function() {
          return metadata;
        }

        // Sets terminology object
        // and performs lookup of all related metadata
        this.setTerminology = function(terminology) {
          console.debug("setTerminology", terminology);

          var deferred = $q.defer();

          // get metadata
          gpService.increment();
          $http
            .get(
              metadataUrl + 'all/terminology/' + terminology.terminology + '/'
                + terminology.version)
            .then(
              // success
              function(response) {
                metadata.terminology = terminology;
                metadata.entries = response.data.keyValuePairList;
                metadata.relationshipTypes = null;
                metadata.attributeNames = null;
                metadata.termTypes = null;
                metadata.generalEntries = null;
                metadata.labelSets = null;

                if (metadata.terminology == null)
                  return;

                for (var i = 0; i < metadata.entries.length; i++) {
                  // extract relationship types for
                                    // convenience
                  if (metadata.entries[i].name === 'Relationship_Types') {
                    metadata.relationshipTypes = metadata.entries[i].keyValuePair;
                  }
                  if (metadata.entries[i].name === 'Attribute_Names') {
                    metadata.attributeNames = metadata.entries[i].keyValuePair;
                  }
                  if (metadata.entries[i].name === 'Term_Types') {
                    metadata.termTypes = metadata.entries[i].keyValuePair;
                  }
                  if (metadata.entries[i].name === 'Label_Sets') {
                    metadata.labelSets = metadata.entries[i].keyValuePair;
                  }
                  if (metadata.entries[i].name === 'General_Metadata_Entries') {
                    metadata.generalEntries = metadata.entries[i].keyValuePair;

                    for (var j = 0; j < metadata.generalEntries.length; j++) {
                      if (metadata.generalEntries[j].key === "Atoms_Label") {
                        metadata.atomsLabel = metadata.generalEntries[j].value;
                      }
                      if (metadata.generalEntries[j].key === "Hierarchies_Label") {
                        metadata.hierarchiesLabel = metadata.generalEntries[j].value;
                      }
                      if (metadata.generalEntries[j].key === "Definitions_Label") {
                        metadata.definitionsLabel = metadata.generalEntries[j].value;
                      }
                      if (metadata.generalEntries[j].key === "Attributes_Label") {
                        metadata.attributesLabel = metadata.generalEntries[j].value;
                      }
                      if (metadata.generalEntries[j].key === "Subsets_Label") {
                        metadata.subsetsLabel = metadata.generalEntries[j].value;
                      }
                      if (metadata.generalEntries[j].key === "Relationships_Label") {
                        metadata.relationshipsLabel = metadata.generalEntries[j].value;
                      }
                      if (metadata.generalEntries[j].key === "Atom_Relationships_Label") {
                        metadata.atomRelationshipsLabel = metadata.generalEntries[j].value;
                      }
                      if (metadata.generalEntries[j].key === "Extensions_Label") {
                        metadata.extensionsLabel = metadata.generalEntries[j].value;
                      }
                      if (metadata.generalEntries[j].key === "Tree_Sort_Field") {
                        metadata.treeSortField = metadata.generalEntries[j].value;
                      }
                    }
                  }
                }
                gpService.decrement();
                deferred.resolve(response.data);
              },
              // error
              function(response) {
                utilService.handleError(response);
                gpService.decrement();
                deferred.reject(response.data);
              });

          // get precedence
          var deferred2 = $q.defer();
          gpService.increment();
          $http.get(
            metadataUrl + 'precedence/' + terminology.terminology + '/'
              + terminology.version).then(
          // success
          function(response) {
            metadata.precedenceList = response.data.precedence;
            gpService.decrement();
            deferred2.resolve("");
          },
          // error
          function(response) {
            utilService.handleError(response);
            gpService.decrement();
            deferred2.reject("");
          });

          // Return all deferred promises
          return $q.all([ deferred.promise, deferred2.promise ]);
        }

        // Returns the terminology object for the terminology name
        this.getTerminology = function(terminology, version) {
          // check for full terminology object by comparing to
                    // selected
          // terminology
          if (terminology != metadata.terminology.terminology) {

            // cycle over available terminologies for match
            for (var i = 0; i < metadata.terminologies.length; i++) {
              if (metadata.terminologies[i].terminology === terminology) {

                // skip if version is set and does not match
                if (!version || version != metadata.terminologies[i].version) {
                  continue;
                }

                return metadata.terminologies[i];
              }
            }
          } else {
            return metadata.terminology;
          }
        }

        // initialize all terminologies
        this.initTerminologies = function() {
          console.debug("initTerminologies");
          var deferred = $q.defer();

          // Get terminologies
          gpService.increment()
          $http.get(metadataUrl + 'terminology/terminologies').then(
          // success
          function(response) {
            console.debug("  terminologies = ", response.data);
            metadata.terminologies = response.data.terminology;
            gpService.decrement();
            deferred.resolve(response.data);
          },
          // error
          function(response) {
            utilService.handleError(response);
            gpService.decrement();
            deferred.reject(response.data);
          });
          return deferred.promise;
        }

        // get relationship type name from its abbreviation
        this.getRelationshipTypeName = function(abbr) {
          for (var i = 0; i < metadata.relationshipTypes.length; i++) {
            if (metadata.relationshipTypes[i].key === abbr) {
              return metadata.relationshipTypes[i].value;
            }
          }
          return null;
        }

        // get attribute name name from its abbreviation
        this.getAttributeNameName = function(abbr) {
          for (var i = 0; i < metadata.attributeNames.length; i++) {
            if (metadata.attributeNames[i].key === abbr) {
              return metadata.attributeNames[i].value;
            }
          }
          return null;
        }

        // get term type name from its abbreviation
        this.getTermTypeName = function(abbr) {
          for (var i = 0; i < metadata.termTypes.length; i++) {
            if (metadata.termTypes[i].key === abbr) {
              return metadata.termTypes[i].value;
            }
          }
          return null;
        }

        // get general entry name from its abbreviation
        this.getGeneralEntryValue = function(abbr) {
          for (var i = 0; i < metadata.generalEntries.length; i++) {
            if (metadata.generalEntries[i].key === abbr) {
              return metadata.generalEntries[i].value;
            }
          }
          return null;
        }

        // get label sets name
        this.getLabelSetName = function(abbr) {
          for (var i = 0; i < metadata.labelSets.length; i++) {
            if (metadata.labelSets[i].key === abbr) {
              return metadata.labelSets[i].value;
            }
          }
          return null;
        }

        this.isDerivedLabelSet = function(tree) {
          for (var i = 0; i < tree.labels.length; i++) {
            if (tree.labels[i].startsWith("LABELFOR")) {
              return true;
            }
          }
          return false;
        }

        this.isLabelSet = function(tree) {
          for (var i = 0; i < tree.labels.length; i++) {
            if (!tree.labels[i].startsWith("LABELFOR:")) {
              return true;
            }
          }
          return false;
        }

        this.getDerivedLabelSetsValue = function(tree) {
          if (tree.labels == undefined) {
            return;
          }
          var retval = "Ancestor of content in:<br>";
          var j = 0;
          for (var i = 0; i < tree.labels.length; i++) {
            var name = this.getLabelSetName(tree.labels[i]);
            if (tree.labels[i].startsWith("LABELFOR")) {
              if (j++ > 0) {
                retval += "<br>";
              }
              retval += "&#x2022;&nbsp;" + name;
            }
          }
          return retval;
        }

        this.getLabelSetsValue = function(tree) {
          if (tree.labels == undefined) {
            return;
          }
          var retval = "Content in:<br>";
          var j = 0;
          for (var i = 0; i < tree.labels.length; i++) {
            var name = this.getLabelSetName(tree.labels[i]);
            if (!tree.labels[i].startsWith("LABELFOR")) {
              if (j++ > 0) {
                retval += "<br>";
              }
              retval += "&#x2022;&nbsp;" + name;
            }
          }
          return retval;
        }

        this.countLabels = function(component) {
          var retval = 0;
          if (typeof component == "undefined" || !component) {
            return 0;
          }
          if (typeof component.labels == "undefined") {
            return 0;
          }
          for (var i = 0; i < component.labels.length; i++) {
            if (!component.labels[i].startsWith("LABELFOR")) {
              retval++;
            }
          }
          return retval;
        }

      }

    ]);