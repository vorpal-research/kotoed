import kotlinx.warnings.Warnings.UNUSED_PARAMETER
import org.jetbrains.research.kotoed.database.Public

fun main(@Suppress(UNUSED_PARAMETER) args: Array<String>) {

    for (table in Public.PUBLIC.tables) {
        for (field in table.fields()) {
            """
ALTER TABLE ${table.name}
    RENAME COLUMN ${field.name} TO ${field.name};
""".also { println(it) }
        }
        """
ALTER TABLE ${table.name}
    RENAME TO ${table.name};
""".also { println(it) }
    }

    for (seq in Public.PUBLIC.sequences) {
        """
ALTER SEQUENCE ${seq.name}
    RENAME TO ${seq.name};
""".also { println(it) }
    }

}
