package kz.kbtu.furnistore.domain

import kz.kbtu.furnistore.domain.model.Chair
import kz.kbtu.furnistore.domain.model.Material
import kz.kbtu.furnistore.domain.model.Product
import kz.kbtu.furnistore.domain.model.Sofa
import kz.kbtu.furnistore.domain.model.Table
import kz.kbtu.furnistore.domain.model.Wardrobe

/** Demo data: the same catalog as in HW1, plus initial stock per product type. */
object SampleData {

    val cities: List<String> = listOf("Almaty", "Astana", "Shymkent")

    fun catalog(): List<Product> = listOf(
        Sofa(1, "Oslo sofa", 450_000.0, Material.FABRIC, weightKg = 85.0, seats = 3),
        Sofa(2, "Nord sofa", 320_000.0, Material.LEATHER, weightKg = 60.0, seats = 2),
        Table(3, "Astana dining table", 280_000.0, Material.OAK, weightKg = 48.0, lengthCm = 180, widthCm = 90),
        Table(4, "Cafe table", 65_000.0, Material.PINE, weightKg = 12.0, lengthCm = 70, widthCm = 70),
        Chair(5, "Loft chair", 25_000.0, Material.METAL, weightKg = 5.5, hasArmrests = false),
        Chair(6, "Comfort chair", 42_000.0, Material.FABRIC, weightKg = 7.0, hasArmrests = true),
        Wardrobe(7, "Almaty wardrobe", 390_000.0, Material.OAK, weightKg = 120.0, doors = 3),
        Wardrobe(8, "Mini wardrobe", 210_000.0, Material.PINE, weightKg = 70.0, doors = 2),
        Chair(9, "Garden chair", 18_000.0, Material.METAL, weightKg = 4.0, hasArmrests = true),
        Table(10, "Writing desk", 120_000.0, Material.OAK, weightKg = 30.0, lengthCm = 120, widthCm = 60),
    )

    /** Initial stock depends on the product type — `when` with `is` checks (smart casts). */
    fun initialStock(product: Product): Int = when (product) {
        is Chair -> 12
        is Table -> 4
        is Sofa -> 2
        is Wardrobe -> 3
        else -> 1
    }
}
