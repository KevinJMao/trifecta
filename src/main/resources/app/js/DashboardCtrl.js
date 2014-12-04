/**
 * Trifecta Dashboard Controller
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
(function () {
    angular.module('trifecta')
        .controller('DashboardCtrl', ['$scope', '$interval', '$log', '$parse', '$timeout', 'DashboardSvc', 'MessageSearchSvc',
            function ($scope, $interval, $log, $parse, $timeout, DashboardSvc, MessageSearchSvc) {

                $scope.version = "0.18.1";
                $scope.consumerMapping = [];
                $scope.topics = [];
                $scope.hideEmptyTopics = false;
                $scope.loading = 0;

                clearMessage();

                $scope.tabs = [
                    {
                        "name": "Observe",
                        "imageURL": "/app/images/tabs/main/observe.png",
                        "active": false
                    }, {
                        "name": "Inspect",
                        "imageURL": "/app/images/tabs/main/inspect.png",
                        "active": false
                    }, {
                        "name": "Query",
                        "imageURL": "/app/images/tabs/main/queries.png",
                        "active": false
                    }
                ];

                // select the default tab and make it active
                $scope.tab = $scope.tabs[0];
                $scope.tab.active = true;

                /**
                 * Changes the active tab
                 * @param index the given tab index
                 * @param event the given click event
                 */
                $scope.changeTab = function (index, event) {
                    // deactivate the current tab
                    $scope.tab.active = false;

                    // activate the new tab
                    $scope.tab = $scope.tabs[index];
                    $scope.tab.active = true;

                    if (event) {
                        event.preventDefault();
                    }
                };

                /**
                 * Converts the given offset from a string value to an integer
                 * @param partition the partition that the offset value will be updated within
                 * @param offset the given offset string value
                 */
                $scope.convertOffsetToInt = function(partition, offset) {
                    partition.offset = parseInt(offset);
                };

                $scope.messageFinderPopup = function () {
                    MessageSearchSvc.finderDialog($scope).then(function (form) {
                        $log.info("form = " + angular.toJson(form));
                        if(!form || !form.topic) {
                            $scope.addErrorMessage("No topic selected")
                        }
                        else if(!form.criteria) {
                            $scope.addErrorMessage("No criteria specified")
                        }
                        else {
                            form.topic = form.topic.topic;
                            if (form.topic && form.criteria) {
                                // display the loading dialog
                                var loadingDialog = MessageSearchSvc.loadingDialog($scope);

                                // perform the search
                                DashboardSvc.findOne(form.topic, form.criteria)
                                    .then(function (message) {
                                        loadingDialog.close({});
                                        $log.info("message = " + angular.toJson(message));
                                        if (message.type == "error") {
                                            $scope.addErrorMessage(message.message);
                                        }
                                        else {
                                            $scope.message = message;

                                            // find the topic and partition
                                            var myTopic = findTopicByName(message.topic);
                                            if (myTopic) {
                                                var myPartition = findPartitionByID(myTopic, message.partition);
                                                if (myPartition) {
                                                    $scope.topic = myTopic;
                                                    $scope.partition = myPartition;
                                                    $scope.partition.offset = message.offset
                                                }
                                            }
                                        }
                                    });
                            }
                        }
                    });
                };

                $scope.getTopics = function (hideEmptyTopics) {
                    return $scope.topics.filter(function (topic) {
                        return !hideEmptyTopics || topic.totalMessages > 0;
                    });
                };

                $scope.loadMessage = function () {
                    if ($scope.topic.totalMessages > 0) {
                        var topic = $scope.topic.topic;
                        var partition = $scope.partition.partition;
                        var offset = $scope.partition.offset;

                        clearMessage();

                        $scope.loading++;

                        $log.info("Loading message for topic=" + topic + ", partition=" + partition + ", offset=" + offset);
                        DashboardSvc.getMessage(topic, partition, offset).then(
                            function (message) {
                                $scope.message = message;
                                if($scope.loading) $scope.loading--;
                            },
                            function (err) {
                                $scope.addError(err);
                                if($scope.loading) $scope.loading--;
                            });
                    }
                };

                $scope.firstMessage = function () {
                    ensureOffset($scope.partition);
                    if ($scope.partition.offset != $scope.partition.startOffset) {
                        $scope.partition.offset = $scope.partition.startOffset;
                        $scope.loadMessage();
                    }
                };

                $scope.lastMessage = function () {
                    ensureOffset($scope.partition);
                    if ($scope.partition.offset != $scope.partition.endOffset) {
                        $scope.partition.offset = $scope.partition.endOffset;
                        $scope.loadMessage();
                    }
                };

                $scope.medianMessage = function() {
                    ensureOffset($scope.partition);
                    var median = Math.round($scope.partition.endOffset/2);
                    if ($scope.partition.offset != median) {
                        $scope.partition.offset = median;
                        $scope.loadMessage();
                    }
                };

                $scope.nextMessage = function () {
                    ensureOffset($scope.partition);
                    var offset = $scope.partition.offset;
                    if (offset < 1 + $scope.partition.endOffset) {
                        $scope.partition.offset += 1;
                        $scope.loadMessage();
                    }
                };

                $scope.previousMessage = function () {
                    ensureOffset($scope.partition);
                    var offset = $scope.partition.offset;
                    if (offset && offset > $scope.partition.startOffset) {
                        $scope.partition.offset -= 1;
                        $scope.loadMessage();
                    }
                };

                $scope.switchToMessage = function (topicID, partitionID, offset) {
                    $log.info("switchToMessage: topicID = " + topicID + ", partitionID = " + partitionID + ", offset = " + offset);
                    var topic = findTopicByName(topicID);
                    var partition = topic ? findPartitionByID(topic, partitionID) : null;
                    if (partition) {
                        $scope.topic = topic;
                        $scope.partition = partition;
                        $scope.partition.offset = offset;
                        $scope.loadMessage();
                        $scope.changeTab(1, null); // Query
                    }
                };

                var _lastGoodResult = "";
                $scope.toPrettyJSON = function (objStr, tabWidth) {
                    try {
                        var obj = $parse(objStr)({});
                    } catch (e) {
                        // eat $parse error
                        return _lastGoodResult;
                    }

                    var result = JSON.stringify(obj, null, Number(tabWidth));
                    _lastGoodResult = result;

                    return result;
                };

                $scope.updatePartition = function (partition) {
                    $scope.partition = partition;

                    // if the current offset is not set, set it at the starting offset.
                    ensureOffset(partition);

                    // load the first message
                    if ($scope.topic.totalMessages > 0 && $scope.partition.offset) {
                        $scope.loadMessage();
                    }
                    else {
                        clearMessage();
                    }
                };

                $scope.updateTopic = function (topic) {
                    $scope.topic = topic;

                    var partitions = topic != null ? topic.partitions : null;
                    if (partitions) {
                        $scope.updatePartition(partitions[0]);
                    }
                    else {
                        console.log("No partitions found");
                        $scope.partition = {};
                        clearMessage();
                    }
                };

                function clearMessage() {
                    $scope.message = {};
                }

                function ensureOffset(partition) {
                    if(!partition.offset) {
                        partition.offset = partition.startOffset;
                    }
                }

                /**
                 * Filters out topics without messages; returning only the topics containing messages
                 * @param topics the given array of topic summaries
                 * @returns the array of topics containing messages
                 */
                $scope.filterEmptyTopics = function (topics) {
                    var filteredTopics = [];
                    for (var n = 0; n < topics.length; n++) {
                        var ts = topics[n];
                        if (ts.totalMessages > 0) {
                            filteredTopics.push(ts);
                        }
                    }
                    return filteredTopics;
                };

                /**
                 * Attempts to find and return the first non-empty topic; however, if none are found, it returns the
                 * first topic in the array
                 * @param topicSummaries the given array of topic summaries
                 * @returns the first non-empty topic
                 */
                function findNonEmptyTopic(topicSummaries) {
                    for (var n = 0; n < topicSummaries.length; n++) {
                        var ts = topicSummaries[n];
                        if (ts.totalMessages > 0) {
                            return ts;
                        }
                    }
                    return topicSummaries.length > 0 ? topicSummaries[0] : null;
                }

                function findPartitionByID(topic, partitionId) {
                    var partitions = topic.partitions;
                    for (var n = 0; n < partitions.length; n++) {
                        if (partitions[n].partition == partitionId) return partitions[n];
                    }
                    return null;
                }

                function findTopicByName(topicId) {
                    var topics = $scope.topics;
                    for (var n = 0; n < topics.length; n++) {
                        if (topics[n].topic == topicId) return topics[n];
                    }
                    return null;
                }

                $scope.$watch("Topics.topics", function(newTopics, oldTopics) {
                    $log.info("DashboardCtrl: Loaded new topics (" + newTopics.length + ")");
                    $scope.topics = newTopics;
                    $scope.updateTopic(findNonEmptyTopic($scope.topics));
                });

            }]);

})();