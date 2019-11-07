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

            [mount.core :refer [defstate]]))

(defn query [])

(defn query-economic-event
  [id]
  #_(let [event (jdbc/execute-one! db
                                 ["select * from EconomicEvent where id = ?" id]
                                 {:builder-fn opt/as-unqualified-maps})
        effortQuantityUnit (query :Unit (:resourceQuantityUnit event))
        action (query :Action (:action event))
        provider (query :Agent (:provider event))
        receiver (query :Agent (:receiver event))
        resourceInventoriedAs (query :EconomicResource (:resourceInventoriedAs event))
        resourceConformsTo  (query :ResourceSpecification (:resourceConformsTo event))
        inputOf (query :Process (:inputOf event))
        outputOf (query :Process (:outputOf event))]
    (-> event
        (merge {:inputOf inputOf})
        (merge {:outputOf outputOf})
        (merge {:effortQuantityUnit effortQuantityUnit})
        (merge {:action action})
        (merge {:provider provider})
        (merge {:receiver receiver})
        (merge {:resourceConformsTo resourceConformsTo})
        (merge {:resourceInventoriedAs resourceInventoriedAs}))))


(defn query-all-economic-event 
  []
  #_(let [ids (jdbc/execute! db
                           ["select id from EconomicEvent"]
                           {:builder-fn opt/as-unqualified-maps})]
    (map #(query-economic-event (:id %)) ids)))


(defn query-economic-resource
  [id]
  #_(let [resource (jdbc/execute-one! db
                                    ["select * from EconomicResource where id = ?" id]
                                    {:builder-fn opt/as-unqualified-maps})
        accountingQuantityUnit (query :Unit (:accountingQuantityUnit resource))
        unitOfEffort (query :Unit (:unitOfEffort resource))
        conformsTo (query :ResourceSpecification (:conformsTo resource))
        onHandQuantityUnit (query :Unit (:onHandQuantityUnit resource))
        owner (query :Agent (:owner resource))]
    (-> resource
        (merge {:accountingQuantityUnit accountingQuantityUnit})
        (merge {:unitOfEffort unitOfEffort})
        (merge {:conformsTo conformsTo})
        (merge {:onHandQuantityUnit onHandQuantityUnit})
        (merge {:owner owner}))))

(defn query-all-economic-resource 
  []
  #_(let [ids (jdbc/execute! db
                           ["select id from EconomicResource"]
                           {:builder-fn opt/as-unqualified-maps})]
    (map #(query-economic-resource (:id %)) ids)))


(defn get-process-inputs
  [id]
  #_(let [ids (jdbc/execute! db
                           ["select id from EconomicEvent where inputOf = ?" id]
                           {:builder-fn opt/as-unqualified-maps})]
    (map #(query-economic-event (:id %)) ids)))

(defn get-process-outputs
  [id]
  #_(let [ids (jdbc/execute! db
                           ["select id from EconomicEvent where outputOf = ?" id]
                           {:builder-fn opt/as-unqualified-maps})]
    (map #(query-economic-event (:id %)) ids)))


(defn get-agent-economic-event
  [id]
  #_(let [ids (jdbc/execute! db
                           ["select id from EconomicEvent where provider = ?" id]
                           {:builder-fn opt/as-unqualified-maps})]
    (map #(query-economic-event (:id %)) ids)))

(defn get-agent-inventoriedEconomicResource
  [id]
  #_(let [ids (jdbc/execute! db
                           ["select id from EconomicResource where owner = ?" id]
                           {:builder-fn opt/as-unqualified-maps})]
    (map #(query :EconomicResource (:id %)) ids)))


                                        ; ; MUTATIONS TODO
                                        ; (defn create-economic-event
                                        ;   [event]
                                        ;   (insert! db
                                        ;                 :EconomicEvent
                                        ;                 {:action (:action event)
                                        ;                  :resourceQuantityNumericValue (:resourceQuantityNumericValue event) 
                                        ;                  :resourceQuantityUnit (:resourceQuantityUnit event)
                                        ;                  :effortQuantityNumericValue (:effortQuantityNumericValue event)
                                        ;                  :effortQuantityUnit (:effortQuantityUnit event) 
                                        ;                  :hasPointInTime (:hasPointInTime event)
                                        ;                  :note (:note event)
                                        ;                  :provider (:provider event)
                                        ;                  :receiver (:receiver event)
                                        ;                  :resourceInventoriedAs (:resourceInventoriedAs event)
                                        ;                  :resourceConformsTo (:resourceConformsTo event)
                                        ;                  :inputOf (:inputOf event)
                                        ;                  :outputOf (:outputOf event)
                                        ;                  :toResourceInventoriedAs (:toResourceInventoriedAs event)}))


