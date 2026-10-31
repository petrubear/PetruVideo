package com.petruvideo.core.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule

/**
 * Base class for migration tests. Every schema change ships with a test that opens the previous version
 * from `core/data/schemas`, runs the migration, and validates the result. Destructive migration is not allowed.
 */
abstract class MigrationTestBase {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PetruDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )
}
