package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.luxion.fitarchiveapp.avatar.AspectoAvatar
import app.luxion.fitarchiveapp.avatar.EstiloPelo
import app.luxion.fitarchiveapp.db.Avatar
import app.luxion.fitarchiveapp.db.FitArchiveDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

interface AvatarRepository {
    /** Emite el aspecto guardado, o el de por defecto mientras no haya ninguno. */
    fun observe(): Flow<AspectoAvatar>
    suspend fun get(): AspectoAvatar
    suspend fun save(aspecto: AspectoAvatar)
}

internal class SqlAvatarRepository(
    db: FitArchiveDatabase,
    private val context: CoroutineContext = Dispatchers.Default,
) : AvatarRepository {
    private val queries = db.avatarQueries

    override fun observe() = queries.select().asFlow().mapToOneOrNull(context).map { it?.aAspecto() ?: AspectoAvatar() }

    override suspend fun get() = withContext(context) { queries.select().executeAsOneOrNull()?.aAspecto() ?: AspectoAvatar() }

    override suspend fun save(aspecto: AspectoAvatar) = withContext(context) {
        queries.upsert(
            skin_rgb = aspecto.pielRgb.toLong(),
            hair_rgb = aspecto.peloRgb.toLong(),
            hair_style = aspecto.estiloPelo.name,
            face_photo_path = aspecto.fotoRostro,
            use_photo = if (aspecto.usarFoto) 1L else 0L,
        )
        Unit
    }
}

private fun Avatar.aAspecto() = AspectoAvatar(
    pielRgb = skin_rgb.toInt(),
    peloRgb = hair_rgb.toInt(),
    estiloPelo = EstiloPelo.entries.firstOrNull { it.name == hair_style } ?: EstiloPelo.LARGO,
    fotoRostro = face_photo_path,
    usarFoto = use_photo == 1L,
)
