;; TODO

;; Copyright (C) 2019- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software; you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

;; Additional permission under GNU AGPL version 3 section 7.

;; If you modify this Program, or any covered work, by linking or combining it with any library (or a modified version of that library), containing parts covered by the terms of EPL v 1.0, the licensors of this Program grant you additional permission to convey the resulting work. Your modified version must prominently offer all users interacting with it remotely through a computer network (if your version supports such interaction) an opportunity to receive the Corresponding Source of your version by providing access to the Corresponding Source from a network server at no charge, through some standard or customary means of facilitating copying of software. Corresponding Source for a non-source form of such a combination shall include the source code for the parts of the libraries (dependencies) covered by the terms of EPL v 1.0 used as well as that of the covered work.

(ns valueflows.db.queries
  (:require [taoensso.timbre :as log]
            [valueflows.stores :refer [stores]]
            [clj-storage.core :as store]
            [clojure.pprint :as pp]
            )
  )



(defn query-process
  ([]
   (let [processes (store/query (:process-store stores) {})]
     (map #(let [inputs (store/query (:transaction-store stores) {:inputOf (:processId %)})
                 outputs (store/query (:transaction-store stores) {:outputOf (:processId %)})
                 ]
             (assoc %
                    :inputs inputs
                    :outputs outputs)
             )
            processes)
     )
   )
  (
   [{:keys [process-id]}]
   (let [process (store/query (:process-store stores) {:processId process-id})
         inputs (store/query (:transaction-store stores) {:inputOf process-id})
         outputs (store/query (:transaction-store stores) {:outputOf process-id})
         ]
     (if (empty? process)
       nil
       (assoc (-> process
                  first)
              :inputs inputs
              :outputs outputs
              )
       )
     ))

  )



(defn query-economic-event
  ([]
   (let [allEvents (map #(assoc %
                                :input-of (query-process {:processId (:inputOf %)})
                                :output-of (query-process {:processId (:outputOf %)})
                                ) (store/query (:transaction-store stores) {}))
         ]
     allEvents

     )
   )
  ([{:keys [economic-event-id before after provider receiver action input-of output-of resource-inventory-as resource-conforms-to] :as param-map}]
   (let [economicEvent (-> (store/query (:transaction-store stores) param-map)
                           first)
         inputProcess (query-process {:processId input-of})
         outputProcess (query-process {:processId output-of})
         ]
     (assoc economicEvent
            :inputOf inputProcess
            :outputOf outputProcess
            )
     )
   )
  )

(defn query-resource
  [{:keys [name]}]
  (let [resource (store/query (:transaction-store stores) {:toResourceInventoriedAs name})
        received (store/aggregate (:transaction-store stores) [{"$match" {:toResourceInventoriedAs name}}
                                                      {"$group" {:_id "$receiver"
                                                                 :resourceQuantityHasNumericalValue {"$sum" "$resourceQuantityHasNumericalValue"}}}])
        provided (store/aggregate (:transaction-store stores) [{"$match" {:resourceInventoriedAs name}}
                                                      {"$group" {:_id "$provider"
                                                                 :resourceQuantityHasNumericalValue {"$sum" "$resourceQuantityHasNumericalValue"}}}])]

    (if (empty? resource)
      nil
      {:resourceQuantityHasNumericalValue (- (if (empty? received)
                                               0
                                               (:resourceQuantityHasNumericalValue (first received))
                                               )
                                             (if (empty? provided)
                                               0
                                               (int (:resourceQuantityHasNumericalValue (first provided)))
                                               ))
       :resourceQuantityHasUnit (-> resource
                                    last
                                    :resourceQuantityHasUnit)
       :name name
       :currentLocation (-> resource
                            last
                            :currentLocation)
       }
      )
    ))


(defn query-intent
  [{:keys [provider]}]
  "Returns a list of intents, currently searching only by provider"
  (let [intents (store/query (:intent-store stores) {:provider provider})]
    intents
    )
  )



