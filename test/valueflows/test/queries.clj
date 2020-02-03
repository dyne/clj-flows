;; TODO

;; Copyright (C) 2019- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

;; Additional permission under GNU AGPL version 3 section 7.

;; If you modify this Program, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.

(ns valueflows.test.queries
  (:require [midje.sweet :refer [against-background before after facts fact =>]]
            [mount.core :as mount]
            [clj-storage.core :as storage]
            [valueflows
             [stores :refer [stores]]]
            [valueflows.db
             [queries :as q]
             [mutations :as mut]]
            [taoensso.timbre :as log]
            ))

(against-background [(before :contents (mount/start-with-args {:port 8888}))
                     (after :contents (do
                                        (storage/empty-db-stores! stores)
                                        (mount/stop)))
                     (after :contents (mount/stop))]

                    (facts "Create some processes and events and test query functions"
                           (fact "First insert some economic events and processess"

                                 (mut/create-economic-event :transfer 200 :kilo
                                                            {:economicEventId "raw-material-transfer"
                                                             :receiver "textile-lab"
                                                             :toResourceInventoriedAs "Lot 173 textile material"
                                                             :currentLocation "52.372807,4.8981023"
                                                             :resourceClassifiedAs [:red :cotton]
                                                             :resourceInventoriedAs "Red cotton textile"})
                                 (mut/create-process "Produce jeans using raw material" {:processId "produce-jeans-process"})
                                 (mut/create-economic-event :consume 10 :kilo
                                                            {:economicEventId "consume-some-raw-material"
                                                             :receiver "textile-lab"
                                                             :resourceInventoriedAs "Lot 173 textile material"
                                                             :inputOf "produce-jeans-process"
                                                             })
                                 (mut/create-economic-event :consume 10 :kilo
                                                            {:economicEventId "consume-some-raw-material-2"
                                                             :receiver "textile-lab"
                                                             :resourceInventoriedAs "Lot 173 textile material"
                                                             :inputOf "produce-jeans-process"
                                                             })
                                 (mut/create-economic-event :work 8 :hour
                                                            {:economicEventId "sew-jeans"
                                                             :receiver "textile-lab"
                                                             :inputOf "produce-jeans-process"
                                                             })
                                 (mut/create-economic-event :produce 1 :each
                                                            {:economicEventId "produce-jeans"
                                                             :receiver "textile-lab"
                                                             :resourceInventoriedAs "Slim fit Jeans"
                                                             :currentLocation "52.372807,4.8981023"
                                                             :outputOf "produce-jeans-process"}))
                           (fact "Test list-all-resources"
                                 (q/list-all-resources) => '({:currentLocation nil
                                                              :name "Red cotton textile"
                                                              :resourceId "Red cotton textile"
                                                              :resourceQuantityHasNumericalValue -200.0
                                                              :resourceQuantityHasUnit "kilo"}
                                                             {:currentLocation "52.372807,4.8981023"
                                                              :name "Slim fit Jeans"
                                                              :resourceId "Slim fit Jeans"
                                                              :resourceQuantityHasNumericalValue 1.0
                                                              :resourceQuantityHasUnit "each"}
                                                             {:currentLocation "52.372807,4.8981023"
                                                              :name "Lot 173 textile material"
                                                              :resourceId "Lot 173 textile material"
                                                              :resourceQuantityHasNumericalValue 180.0
                                                              :resourceQuantityHasUnit "kilo"}))

                           (fact "Test query all economic events"
                                 (map #(-> % :resourceInventoriedAs :name) (q/query-economic-event)) => '("Red cotton textile"
                                                                                                          "Lot 173 textile material"
                                                                                                          "Lot 173 textile material"
                                                                                                          nil
                                                                                                          "Slim fit Jeans"))
                           (fact "Test economic-event->process"
                                 (let [process-for-event (q/economic-event->process "sew-jeans")]
                                   (:processId process-for-event) => "produce-jeans-process"
                                   (count (:inputs process-for-event)) => 3
                                   (count (:outputs process-for-event)) => 1))

                           (fact "test query resource"
                                 (q/query-resource {:name "Red cotton textile"}) => {:currentLocation nil
                                                                                     :name "Red cotton textile"
                                                                                     :resourceId "Red cotton textile"
                                                                                     :resourceQuantityHasNumericalValue -200.0
                                                                                     :resourceQuantityHasUnit "kilo"}

                                 )

                           #_(fact "Test process->input-economic-events"
                                 (q/traceback-process "produce-jeans-process") => [{:action "consume"
                                                                                    :economicEventId "consume-some-raw-material"
                                                                                    :provider nil
                                                                                    :receiver "textile-lab"
                                                                                    :resourceQuantityHasNumericalValue 10
                                                                                    :resourceQuantityHasUnit "kilo"}
                                                                                   {:action "consume"
                                                                                    :economicEventId "consume-some-raw-material-2"
                                                                                    :provider nil
                                                                                    :receiver "textile-lab"
                                                                                    :resourceQuantityHasNumericalValue 10
                                                                                    :resourceQuantityHasUnit "kilo"}

                                                                                   {:action "work"
                                                                                    :economicEventId "sew-jeans"
                                                                                    :provider nil
                                                                                    :receiver "textile-lab"
                                                                                    :resourceQuantityHasNumericalValue 8
                                                                                    :resourceQuantityHasUnit "hour"}])
                           #_(fact "Test economic-resource->output-economic-events"
                                 (q/traceback-economic-resource "Red cotton textile")
                                 => [{:action "transfer"
                                     :economicEventId "raw-material-transfer"
                                     :provider nil
                                     :receiver "textile-lab"
                                     :resourceQuantityHasNumericalValue 200
                                     :resourceQuantityHasUnit "kilo"}])

                           #_(fact "Test traceback-economic-event"
                                 (q/traceback-economic-event "produce-jeans") => {:name "Produce jeans using raw material" :processId "produce-jeans-process"}
                                 (q/traceback-economic-event "raw-material-transfer") => {
                                                                                          :name "Red cotton textile"
                                                                                          :resourceId "Red cotton textile"
                                                                                          :resourceQuantityHasNumericalValue -200
                                                                                          :resourceQuantityHasUnit "kilo"}
))

                    #_(facts "Trace scenario"
                           (fact "Given a resource name the whole backtrace tree is returned"
                                 (let [resource-name "Slim fit Jeans"]
                                   (q/trace-resource resource-name)
                                   => {:name "Slim fit Jeans"
                                       :resourceQuantityHasNumericalValue 1
                                       :resourceQuantityHasUnit "each"
                                       :resourceId "Slim fit Jeans"
                                       :associatedEvent  {:action "produce"
                                                          :resourceInventoriedAs "Slim fit Jeans"
                                                          :provider nil
                                                          :receiver "textile-lab"
                                                          :resourceQuantityHasNumericalValue 1
                                                          :resourceQuantityHasUnit "each"
                                                          :economicEventId "produce-jeans"
                                                          :outputOf  {:name "Produce jeans using raw material"
                                                                      :processId "produce-jeans-process"
                                                                      :inputs [{:action "consume"
                                                                                :economicEventId "consume-some-raw-material"
                                                                                :provider nil
                                                                                :receiver "textile-lab"
                                                                                :resourceQuantityHasNumericalValue 10
                                                                                :resourceQuantityHasUnit "kilo"
                                                                                :resourceInventoriedAs "Lot 173 textile material"
                                                                                :associatedResource {:name  "Lot 173 textile material"
                                                                                                     :resourceQuantityHasNumericalValue 180
                                                                                                     :resourceQuantityHasUnit "kilo"
                                                                                                     :resourceId  "Lot 173 textile material"
                                                                                                     :associatedEvent {:action "transfer"
                                                                                                                       :economicEventId "raw-material-transfer"
                                                                                                                       :provider nil
                                                                                                                       :receiver "textile-lab"
                                                                                                                       :resourceQuantityHasNumericalValue 200
                                                                                                                       :resourceQuantityHasUnit "kilo"
                                                                                                                       :outputOf nil
                                                                                                                       :resourceInventoriedAs "Red cotton textile"
                                                                                                                       :toResourceInventoriedAs "Lot 173 textile material"
                                                                                                                       :associatedResource {:name "Red cotton textile"
                                                                                                                                            :resourceId "Red cotton textile"
                                                                                                                                            :resourceQuantityHasNumericalValue -200
                                                                                                                                            :resourceQuantityHasUnit "kilo"
                                                                                                                                            :outputOf nil
                                                                                                                                            :associatedEvent nil
                                                                                                                                            }
                                                                                                                       }
                                                                                                     }
                                                                                }
                                                                               {:action "consume"
                                                                                :economicEventId "consume-some-raw-material-2"
                                                                                :provider nil
                                                                                :receiver "textile-lab"
                                                                                :resourceQuantityHasNumericalValue 10
                                                                                :resourceQuantityHasUnit "kilo"
                                                                                :associatedResource {:name  "Lot 173 textile material"
                                                                                                     :resourceQuantityHasNumericalValue 180
                                                                                                     :resourceQuantityHasUnit "kilo"
                                                                                                     :resourceId  "Lot 173 textile material"
                                                                                                     :associatedEvent {:action "transfer"
                                                                                                                       :economicEventId "raw-material-transfer"
                                                                                                                       :provider nil
                                                                                                                       :receiver "textile-lab"
                                                                                                                       :resourceQuantityHasNumericalValue 200
                                                                                                                       :resourceQuantityHasUnit "kilo"
                                                                                                                       :outputOf nil
                                                                                                                       :resourceInventoriedAs "Red cotton textile"
                                                                                                                       :toResourceInventoriedAs "Lot 173 textile material"
                                                                                                                       :associatedResource {:name "Red cotton textile"
                                                                                                                                            :resourceId "Red cotton textile"
                                                                                                                                            :resourceQuantityHasNumericalValue -200
                                                                                                                                            :resourceQuantityHasUnit "kilo"
                                                                                                                                            :outputOf nil
                                                                                                                                            :associatedEvent nil
                                                                                                                                            }
                                                                                                                       }
                                                                                                     }
                                                                                }
                                                                               {:action "work"
                                                                                :economicEventId "sew-jeans"
                                                                                :provider nil
                                                                                :receiver "textile-lab"
                                                                                :resourceQuantityHasNumericalValue 8
                                                                                :resourceQuantityHasUnit "hour"
                                                                                :outputOf nil
                                                                                :associatedResource nil
                                                                                }]
                                                                      }}

                                       }
                                   ))))
