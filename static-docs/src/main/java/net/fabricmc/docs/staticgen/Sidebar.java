package net.fabricmc.docs.staticgen;

import java.util.List;

final class Sidebar {
	private Sidebar() {
	}

	static String render(BuildContext context, Page page) {
		List<Item> items = page.route().contains("/players/") ? players() : develop();
		String prefix = Html.relativeAssetPrefix(page);
		String localePrefix = page.locale().equals("en_us") ? "" : page.locale();
		StringBuilder out = new StringBuilder("<ul class=\"sidebar-root\">");
		for (Item item : items) append(out, context, page, item, prefix, localePrefix);
		return out.append("</ul>").toString();
	}

	private static void append(StringBuilder out, BuildContext context, Page page, Item item, String prefix, String localePrefix) {
		String text = context.translations().sidebar(page.locale(), item.text);
		String link = item.link == null ? "" : item.link;
		if (!link.isBlank() && !localePrefix.isBlank()) link = "/" + localePrefix + link;
		out.append("<li>");
		boolean activeTrail = activeTrail(page, link, localePrefix, item);
		if (!item.children.isEmpty()) out.append("<details").append(activeTrail ? " open" : "").append("><summary>");
		if (link.isBlank()) {
			out.append("<span>").append(Html.escape(text)).append("</span>");
		} else {
			String href = prefix + link.replaceFirst("^/", "") + (link.endsWith("/") ? "index.html" : ".html");
			out.append("<a");
			if (page.route().equals(link) || page.route().equals("/" + localePrefix + link)) out.append(" class=\"active\"");
			out.append(" href=\"").append(Html.escape(href)).append("\">").append(Html.escape(text)).append("</a>");
		}
		if (!item.children.isEmpty()) {
			out.append("</summary>");
			out.append("<ul>");
			for (Item child : item.children) append(out, context, page, child, prefix, localePrefix);
			out.append("</ul>");
			out.append("</details>");
		}
		out.append("</li>");
	}

	private static boolean activeTrail(Page page, String link, String localePrefix, Item item) {
		if (isActive(page, link, localePrefix)) return true;
		for (Item child : item.children) {
			if (activeTrail(page, child.link, localePrefix, child)) return true;
		}
		return false;
	}

	private static boolean isActive(Page page, String link, String localePrefix) {
		if (link == null || link.isBlank()) return false;
		return page.route().equals(link) || (!localePrefix.isBlank() && page.route().equals("/" + localePrefix + link));
	}

	private static List<Item> players() {
		return List.of(
				item("players.title", "/players/",
						item("players.installing_java", "/players/installing-java/"),
						item("players.installing_fabric", "/players/installing-fabric/"),
						item("players.updating_fabric", "/players/updating-fabric/"),
						item("players.finding_mods", "/players/finding-mods"),
						item("players.installing_mods", "/players/installing-mods"),
						item("players.troubleshooting", null,
								item("players.troubleshooting.uploading_logs", "/players/troubleshooting/uploading-logs"),
								item("players.troubleshooting.crash_reports", "/players/troubleshooting/crash-reports"),
								item("players.troubleshooting.dependency_overrides", "/players/troubleshooting/dependency-overrides")),
						item("players.faq", "/players/faq"))
		);
	}

	private static List<Item> develop() {
		return List.of(
				item("develop.title", "/develop/",
						item("develop.getting_started.creating_project", "/develop/getting-started/creating-a-project"),
						item("develop.getting_started.project_structure", "/develop/getting-started/project-structure"),
						item("develop.getting_started.setting_up", "/develop/getting-started/setting-up"),
						item("develop.getting_started.opening_project", "/develop/getting-started/opening-a-project"),
						item("develop.getting_started.launching_game", "/develop/getting-started/launching-the-game"),
						item("develop.getting_started.generating_sources", "/develop/getting-started/generating-sources"),
						item("develop.getting_started.building_mod", "/develop/getting-started/building-a-mod"),
						item("develop.getting_started.tips_and_tricks", "/develop/getting-started/tips-and-tricks")),
				item("develop.items", null,
						item("develop.items.first_item", "/develop/items/first-item",
								item("develop.items.food", "/develop/items/food"),
								item("develop.items.potions", "/develop/items/potions"),
								item("develop.items.spawn_egg", "/develop/items/spawn-egg"),
								item("develop.items.custom_tools", "/develop/items/custom-tools"),
								item("develop.items.custom_armor", "/develop/items/custom-armor")),
						item("develop.items.item_models", "/develop/items/item-models"),
						item("develop.items.item_appearance", "/develop/items/item-appearance"),
						item("develop.items.custom_creative_tabs", "/develop/items/custom-creative-tabs"),
						item("develop.items.custom_item_interactions", "/develop/items/custom-item-interactions"),
						item("develop.items.custom_enchantment_effects", "/develop/items/custom-enchantment-effects"),
						item("develop.items.custom_data_components", "/develop/items/custom-data-components")),
				item("develop.blocks", null,
						item("develop.blocks.first_block", "/develop/blocks/first-block"),
						item("develop.blocks.block_models", "/develop/blocks/block-models"),
						item("develop.blocks.blockstates", "/develop/blocks/blockstates"),
						item("develop.blocks.block_entities", "/develop/blocks/block-entities",
								item("develop.blocks.block_entity_renderer", "/develop/blocks/block-entity-renderer"),
								item("develop.blocks.block_containers", "/develop/blocks/block-containers"),
								item("develop.blocks.container_menus", "/develop/blocks/container-menus"),
								item("develop.blocks.block_tinting", "/develop/blocks/block-tinting"))),
				item("develop.fluids", null,
						item("develop.fluids.first_fluid", "/develop/fluids/first-fluid")),
				item("develop.entities", null,
						item("develop.entities.first_entity", "/develop/entities/first-entity"),
						item("develop.entities.attributes", "/develop/entities/attributes"),
						item("develop.entities.effects", "/develop/entities/effects"),
						item("develop.entities.damage_types", "/develop/entities/damage-types")),
				item("develop.sounds", null,
						item("develop.sounds.using_sounds", "/develop/sounds/using-sounds"),
						item("develop.sounds.custom", "/develop/sounds/custom"),
						item("develop.sounds.dynamic_sounds", "/develop/sounds/dynamic-sounds")),
				item("develop.commands", null,
						item("develop.commands.basics", "/develop/commands/basics"),
						item("develop.commands.arguments", "/develop/commands/arguments"),
						item("develop.commands.suggestions", "/develop/commands/suggestions")),
				item("develop.rendering", null,
						item("develop.rendering.basic_concepts", "/develop/rendering/basic-concepts"),
						item("develop.rendering.gui_graphics", "/develop/rendering/gui-graphics"),
						item("develop.rendering.hud", "/develop/rendering/hud"),
						item("develop.rendering.world", "/develop/rendering/world"),
						item("develop.rendering.gui", "/develop/rendering/gui",
								item("develop.rendering.gui.custom_screens", "/develop/rendering/gui/custom-screens"),
								item("develop.rendering.gui.custom_widgets", "/develop/rendering/gui/custom-widgets")),
						item("develop.rendering.particles", "/develop/rendering/particles",
								item("develop.rendering.particles.creating_particles", "/develop/rendering/particles/creating-particles"))),
				item("develop.data_generation", null,
						item("develop.data_generation.setup", "/develop/data-generation/setup"),
						item("develop.data_generation.client", null,
								item("develop.data_generation.translations", "/develop/data-generation/translations"),
								item("develop.data_generation.models", null,
										item("develop.data_generation.block_models", "/develop/data-generation/block-models"),
										item("develop.data_generation.item_models", "/develop/data-generation/item-models"))),
						item("develop.data_generation.server", null,
								item("develop.data_generation.advancements", "/develop/data-generation/advancements"),
								item("develop.data_generation.enchantments", "/develop/data-generation/enchantments"),
								item("develop.data_generation.loot_tables", "/develop/data-generation/loot-tables"),
								item("develop.data_generation.recipes", "/develop/data-generation/recipes"),
								item("develop.data_generation.tags", "/develop/data-generation/tags"),
								item("develop.data_generation.world_generation", "/develop/data-generation/world-generation",
										item("develop.data_generation.features", "/develop/data-generation/world-generation/features")))),
				item("develop.serialization", null,
						item("develop.serialization.codecs", "/develop/codecs"),
						item("develop.serialization.data_attachments", "/develop/data-attachments"),
						item("develop.serialization.saved_data", "/develop/saved-data")),
				item("develop.loom", "/develop/loom/",
						item("develop.loom.fabric_api", "/develop/loom/fabric-api"),
						item("develop.loom.options", "/develop/loom/options"),
						item("develop.loom.prod", "/develop/loom/production-run-tasks"),
						item("develop.loom.classpath_groups", "/develop/loom/classpath-groups"),
						item("develop.loom.tasks", "/develop/loom/tasks")),
				item("develop.loader", "/develop/loader/",
						item("develop.loader.fabric.mod.json", "/develop/loader/fabric-mod-json")),
				item("develop.porting", "/develop/porting/",
						item("develop.porting.fabric_api", "/26.1/develop/porting/fabric-api"),
						item("develop.porting.mappings", "/develop/porting/mappings/",
								item("develop.porting.mappings.loom", "/1.21.11/develop/porting/mappings/loom"),
								item("develop.porting.mappings.ravel", "/1.21.11/develop/porting/mappings/ravel"))),
				item("develop.mixins", null,
						item("develop.mixins.bytecode", "/develop/mixins/bytecode")),
				item("develop.class_tweakers", null,
						item("develop.class_tweakers.introduction", "/develop/class-tweakers/"),
						item("develop.class_tweakers.access_widening", "/develop/class-tweakers/access-widening"),
						item("develop.class_tweakers.interface_injection", "/develop/class-tweakers/interface-injection"),
						item("develop.class_tweakers.enum_extension", "/develop/class-tweakers/enum-extension")),
				item("develop.misc", null,
						item("develop.misc.automatic_testing", "/develop/automatic-testing"),
						item("develop.misc.custom_recipe_types", "/develop/custom-recipes"),
						item("develop.misc.debugging", "/develop/debugging"),
						item("develop.misc.events", "/develop/events"),
						item("develop.misc.game_rules", "/develop/game-rules"),
						item("develop.misc.key_mappings", "/develop/key-mappings"),
						item("develop.misc.networking", "/develop/networking"),
						item("develop.misc.resource_conditions", "/develop/resource-conditions"),
						item("develop.misc.statistics", "/develop/statistics"),
						item("develop.misc.text_and_translations", "/develop/text-and-translations"))
		);
	}

	private static Item item(String text, String link, Item... children) {
		return new Item(text, link, List.of(children));
	}

	private record Item(String text, String link, List<Item> children) {
	}
}
