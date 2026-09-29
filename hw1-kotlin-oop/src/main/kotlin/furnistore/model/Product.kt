package furnistore.model

import furnistore.util.tenge

/** Materials a piece of furniture can be made of. */
enum class Material(val label: String) {
    OAK("oak"), PINE("pine"), METAL("metal"), FABRIC("fabric"), LEATHER("leather")
}

// ---------------------------------------------------------------------------
// Interfaces
// ---------------------------------------------------------------------------

/** Anything that can present itself to the user. */
interface Describable {
    fun describe(): String
}

/**
 * Anything that can be delivered to a customer.
 * `deliveryCost` has a default implementation, so implementors may override it
 * only when their delivery differs from the standard rule.
 */
interface Deliverable {
    val weightKg: Double

    fun deliveryCost(distanceKm: Int): Double = weightKg * 50.0 + distanceKm * 20.0
}

// ---------------------------------------------------------------------------
// Base class + inheritance hierarchy
// ---------------------------------------------------------------------------

/**
 * Base class for every product in the catalog.
 * It is `abstract`, so only concrete furniture types below can be instantiated.
 */
abstract class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val material: Material,
) : Describable, Deliverable {

    /** Every product type decides its own assembly time — implemented by subclasses. */
    abstract fun assemblyMinutes(): Int

    /** Concrete products extend this description with their own details. */
    override fun describe(): String = "#$id $name, ${material.label} — ${price.tenge()}"

    override fun toString(): String = describe()
}

class Sofa(
    id: Int,
    name: String,
    price: Double,
    material: Material,
    override val weightKg: Double,
    val seats: Int,
) : Product(id, name, price, material) {

    override fun assemblyMinutes(): Int = 20 + seats * 10

    override fun describe(): String = "${super.describe()} ($seats seats)"
}

class Table(
    id: Int,
    name: String,
    price: Double,
    material: Material,
    override val weightKg: Double,
    val lengthCm: Int,
    val widthCm: Int,
) : Product(id, name, price, material) {

    val areaM2: Double
        get() = lengthCm * widthCm / 10_000.0

    override fun assemblyMinutes(): Int = if (areaM2 > 1.0) 45 else 30

    override fun describe(): String = "${super.describe()} (${lengthCm}x${widthCm} cm)"
}

class Chair(
    id: Int,
    name: String,
    price: Double,
    material: Material,
    override val weightKg: Double,
    val hasArmrests: Boolean,
) : Product(id, name, price, material) {

    override fun assemblyMinutes(): Int = if (hasArmrests) 15 else 10

    override fun describe(): String =
        "${super.describe()} (${if (hasArmrests) "with" else "without"} armrests)"
}

class Wardrobe(
    id: Int,
    name: String,
    price: Double,
    material: Material,
    override val weightKg: Double,
    val doors: Int,
) : Product(id, name, price, material) {

    override fun assemblyMinutes(): Int = 60 * doors

    /** Wardrobes are bulky: the standard delivery rule is multiplied by 1.5. */
    override fun deliveryCost(distanceKm: Int): Double = super.deliveryCost(distanceKm) * 1.5

    override fun describe(): String = "${super.describe()} ($doors doors)"
}
