/*
 * Copyright 2026 Jason Jamieson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkjamies.mesa.features.summary.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.zacsweers.metro.AppScope
import com.jkjamies.mesa.features.summary.domain.SummaryRepository
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "summary_settings")

@ContributesBinding(AppScope::class)
class SummaryRepositoryImpl(
    private val context: Context
) : SummaryRepository {

    private val LAST_VALUE_KEY = intPreferencesKey("last_saved_value")

    override suspend fun saveValue(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_VALUE_KEY] = value
        }
    }

    override fun observeLastValue(): Flow<Int?> {
        return context.dataStore.data
            .map { preferences ->
                preferences[LAST_VALUE_KEY]
            }
    }
}
