package util.feat;

import java.util.Map;

/**
 * The description shown under a meal's name on the Add / Edit Order pages.
 *
 * Main purpose: one place that knows what each menu item is, so every item
 * gets its own text. Items without an entry get a friendly default.
 */
public final class ProductInfo {

    private static final String DEFAULT_DESCRIPTION =
        "A delicious Gudo’s Restobar favorite, freshly prepared for your cravings.";

    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
        Map.entry("Alejandro Balls", "Juicy, golden meatballs in our rich house sauce, a bite-sized crowd favorite."),
        Map.entry("Peanut Butterbonia Pancit Canton", "Stir-fried canton noodles tossed in a creamy peanut sauce with tender chicken and crisp veggies."),
        Map.entry(".Swing Sisig", "Sizzling chopped pork sisig with onions and chili, finished with a squeeze of calamansi."),
        Map.entry("Quiburger", "A classic burger with a juicy Quibod patty, melted cheese, fresh vegetables, crispy bacon, and savory sauces in a sesame bun."),
        Map.entry("Object Oriented Pasta", "Creamy pasta layered with rich sauce, tender meat and herbs, every strand a well-built bite."),
        Map.entry("Lechong Gudo", "Crispy-skinned roast pork with tender, juicy meat, served with its dipping sauces and rice."),
        Map.entry("LinkedHash Brown", "Golden shredded potatoes fried until crunchy outside and soft inside."),
        Map.entry("4-Pillar Fries", "Thick, crispy fries seasoned four ways, a crunchy tribute to the four pillars of snacking."),
        Map.entry("Chili con Claude", "A hearty, slow-simmered chili of beans and spiced beef, served hot and made to share."),
        Map.entry("JNuggets", "Crunchy golden chicken nuggets with a juicy center and a dipping sauce on the side."),
        Map.entry("Snhackathon Package", "The all-night sharing platter: nuggets, fries and hash browns piled high for the whole team."),
        Map.entry("Jandro Cola", "Our ice-cold house cola, bubbly and sweet, made for washing down every meal."),
        Map.entry("San Gudo Light", "A crisp, easy-drinking light beer with a clean, refreshing finish."),
        Map.entry("San Gudo", "The classic Gudo’s lager: full-bodied, golden and best served ice cold."),
        Map.entry("Sam Smoothie", "A thick, fruity blended smoothie, fresh, cold and refreshing."),
        Map.entry("Java Milkshake", "A rich, creamy milkshake with a bold coffee kick and a swirl of cream."),
        Map.entry("Extra Rice", "An extra serving of steaming white rice to go with any meal."),
        Map.entry("Java Rice", "Fragrant garlic-fried rice with a golden, savory kick."),
        Map.entry("BBQ Sauce", "Sweet and smoky barbecue sauce for dipping, drizzling or spreading."),
        Map.entry("Cheese Sauce", "Warm, creamy cheese sauce, perfect with fries, nuggets and burgers."));

    private ProductInfo() {}

    /** One or two sentences about the product. */
    public static String description(Product product) {
        return DESCRIPTIONS.getOrDefault(product.getName(), DEFAULT_DESCRIPTION);
    }
}
