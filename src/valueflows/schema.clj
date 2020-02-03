;; Copyright (C) 2019- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

;; Additional permission under GNU AGPL version 3 section 7.

;; If you modify this Program, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.

(ns valueflows.schema
  "Contains custom resolver and a function to provide the full schema"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [taoensso.timbre :as log]
            [valueflows.db.queries :as q]
            [valueflows.db.mutations :as m]
            [clojure.walk :as w]
            [clojure.edn :as edn])
  (:use
        clojure.pprint)
  )

(defn resolver-map
  []
  {:query/query-process (fn [_ args _] (let [{:keys [id]} args
                                             process (q/query-process {:processId id})]
                                         process
                                         ))

   :query/query-all-processes (fn [_ _ _] (let [allProcesses (q/query-process)]
                                            (log/spy allProcesses)
                                            ))

   :query/query-economic-event (fn [_ args _] (let [{:keys [id]} args
                                                    economicEvent (q/query-economic-event {:economicEventId id})
                                                    ]
                                                economicEvent
                                                ))
   :query/query-all-economic-events (fn [_ _ _] (let [allEvents
                                                      (q/query-economic-event)
                                                  ]
                                                  allEvents
                                                  ))

   :query/query-resource (fn [_ args _] (let [{:keys [name]} args
                                              resource (q/query-resource {:name name})
                                              ]
                                          resource
                                          ))
   :query/query-all-resources (fn [_ _ _] (let [allResources
                                                (q/list-all-resources)]
                                            allResources
                                            ))


   :mutation/create-process (fn [_ args _] (let [{:keys [name processId before note]} (:process args)
                                                 params {:note note
                                                         :before before
                                                         :processId processId}
                                                 ]
                                             (m/create-process name params)
                                             ))
   :mutation/create-economic-event (fn [_ args _] (let [{:keys [action resourceQuantityHasNumericalValue resourceQuantityHasUnit note hasPointInTime provider receiver inputOf outputOf resourceInventoriedAs toResourceInventoriedAs resourceConformsTo economicEventId resourceDescription currentLocation]} (:event args)
                                                        params {:note note
                                                                :economicEventId economicEventId
                                                                :hasPointInTime hasPointInTime
                                                                :provider provider
                                                                :receiver receiver
                                                                :inputOf inputOf
                                                                :currentLocation currentLocation
                                                                :outputOf outputOf
                                                                :toResourceInventoriedAs toResourceInventoriedAs
                                                                :resourceInventoriedAs resourceInventoriedAs
                                                                :resourceDescription resourceDescription
                                                                :resourceConformsTo resourceConformsTo}
                                                        ]
                                                    (m/create-economic-event action resourceQuantityHasNumericalValue resourceQuantityHasUnit params)))
   })

(defn load-schema
  []
  (-> (io/resource "process-schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))
