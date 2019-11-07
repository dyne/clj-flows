;; TODO

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

            [valueflows.db.queries :refer [
                                          query
                                          get-process-inputs
                                          get-process-outputs
                                          query-economic-resource
                                          query-economic-event
                                          query-all-economic-event
                                          query-all-economic-resource
                                          get-agent-inventoriedEconomicResource
                                          get-agent-economic-event]]
            [clojure.edn :as edn]))

(defn resolver-map
  []
  {:query/process (fn [_ args _] (let [{:keys [id]} args]
                                   (query :Process id)))
   :query/get-inputs (fn [_ _ value] (let [{:keys [id]} value]
                                       (get-process-inputs id)))
   :query/get-outputs (fn [_ _ value] (let [{:keys [id]} value]
                                        (get-process-outputs id)))
   :query/get-agent-economic-event (fn [_ _ value] (let [{:keys [id]} value]
                                                     (get-agent-economic-event id)))

   :query/get-agent-inventoriedEconomicResource (fn [_ _ value] (let [{:keys [id]} value]
                                                                  (get-agent-inventoriedEconomicResource id)))
   :query/allProcesses (fn [_ _ _] (query :Process))
   :query/agent (fn [_ args _] (let [{:keys [id]} args]
                                 (query :Agent id)))
   :query/allAgents (fn [_ _ _] (query :Agent))
   :query/economicEvent (fn [_ args _] (let [{:keys [id]} args]
                                         (query-economic-event id)))
   :query/allEconomicEvents (fn [_ _ _] (query-all-economic-event))
   :query/economicResource (fn [_ args _] (let [{:keys [id]} args]
                                            (query-economic-resource id)))
   :query/allEconomicResources (fn [_ _ _] (query-all-economic-resource))
   :query/resourceSpecification (fn [_ args _] (let [{:keys [id]} args]
                                                 (query :ResourceSpecification id)))
   :query/allResourceSpecification (fn [_ _ _] (query :ResourceSpecification))
   :query/action (fn [_ args _] (let [{:keys [id]} args]
                                  (query :Action id)))
   :query/allActions (fn [_ _ _] (query :Action))
   :query/unit (fn [_ args _] (let [{:keys [id]} args]
                                (query :Unit id)))
   :query/allUnits (fn [_ _ _] (query :Unit))
                                        ;  :mutation/createEconomicEvent (fn [_ args _] (println args))
   })

(defn load-schema
  []
  (-> (io/resource "process-schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))
