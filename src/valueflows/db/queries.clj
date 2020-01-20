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
   ))

(defn query-economic-event
  ([]
   (store/query (:transaction-store stores) {})
   )
  ([{:keys [before after provider receiver action input-of output-of resource-inventory-as resource-conforms-to] :as param-map}]
   (store/query (:transaction-store stores) param-map)
   )
  )

(defn query-resource
  [{:keys [name]}]
  "Track how much of a specific resource exists in the network (check :resource-inventoried-as if received and :to-resource-inventoried-as if provided.)"
  (let [resource (store/query (:transaction-store stores) {:to-resource-inventoried-as name})
        received (store/aggregate (:transaction-store stores) [{"$match" {:to-resource-inventoried-as name}}
                                                      {"$group" {:_id "$receiver"
                                                                 :resource-quantity-has-numerical-value {"$sum" "$resource-quantity-has-numerical-value"}}}])
        provided (store/aggregate (:transaction-store stores) [{"$match" {:resource-inventoried-as name}}
                                                      {"$group" {:_id "$provider"
                                                                 :resource-quantity-has-numerical-value {"$sum" "$resource-quantity-has-numerical-value"}}}])]

    (if (nil? (first received))
      0
      (int (:resource-quantity-has-numerical-value (first received)))
      )
    (if (nil? (first provided))
      0
      (int (:resource-quantity-has-numerical-value (first received)))
      )
    {:resource-quantity-has-numerical-value (- (if (nil? (first received))
                                                 0
                                                 (int (:resource-quantity-has-numerical-value (first received)))
                                                 )
                                               (if (nil? (first provided))
                                                 0
                                                 (int (:resource-quantity-has-numerical-value (first provided)))
                                                 ))
     :name name
     :current-location (-> resource
                           last
                           :current-location)
     }))


(defn query-intent
  [{:keys [provider]}]
  "Returns a list of intents, currently searching only by provider"
  (let [intents (store/query (:intent-store stores) {:provider provider})]
    intents
    )
  )



(defn query-process
  ([]
   (let [processes (store/query (:process-store stores) {})]
     processes
     )
   )
  (
   [{:keys [process-id]}]
   (let [process (store/query (:process-store stores) {:process-id process-id})
         inputs (store/query (:transaction-store stores) {:input-of process-id})
         outputs (store/query (:transaction-store stores) {:output-of process-id})
         ]
     (assoc (-> process
                first)
            :inputs inputs
            :outputs outputs
            )
     ))

  )
