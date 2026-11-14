package com.generaciones;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import com.generaciones.block.GeneradorBlock;
import com.generaciones.block.GeneradorBlockEntity;
import com.generaciones.item.GeneradorItem;
import com.generaciones.item.NucleoItem;
import com.generaciones.item.TooltipItem;
import com.generaciones.item.TooltipBlockItem;
import com.generaciones.machine.ColectorEntity;
import com.generaciones.machine.GranjaEntity;
import com.generaciones.machine.MachineBlock;
import com.generaciones.machine.MachineEntity;
import com.generaciones.machine.MineroEntity;
import com.generaciones.boss.CorazonItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Registro de todo el contenido del mod. */
public final class ModContent {

	// ---------------------------------------------------------------- bloque

	public static final ResourceKey<Block> GENERADOR_KEY = ResourceKey.create(Registries.BLOCK, Generaciones.id("generador"));
	public static final Block GENERADOR = Registry.register(BuiltInRegistries.BLOCK, GENERADOR_KEY,
			new GeneradorBlock(BlockBehaviour.Properties.of()
					.setId(GENERADOR_KEY)
					.strength(2.5f, 6.0f)
					.sound(SoundType.METAL)
					.lightLevel(GeneradorBlock::light)));

	public static final Item GENERADOR_ITEM = item("generador",
			p -> new GeneradorItem(GENERADOR, p), new Item.Properties().useBlockDescriptionPrefix());

	public static final BlockEntityType<GeneradorBlockEntity> GENERADOR_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE, Generaciones.id("generador"),
			FabricBlockEntityTypeBuilder.create(GeneradorBlockEntity::new, GENERADOR).build());

	// ---------------------------------------------------------------- chispas

	public static final Item CHISPA = item("chispa",
			p -> new TooltipItem(p, "tooltip.generaciones.chispa"), new Item.Properties());
	public static final Item PAQUETE_CHISPAS = item("paquete_chispas",
			p -> new TooltipItem(p, "tooltip.generaciones.paquete", "tooltip.generaciones.paquete.usos"), new Item.Properties());
	public static final Item CRISTAL_CHISPAS = item("cristal_chispas",
			p -> new TooltipItem(p, "tooltip.generaciones.cristal", "tooltip.generaciones.cristal.usos"),
			new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item ESTRELLA_CHISPAS = item("estrella_chispas",
			p -> new TooltipItem(p, "tooltip.generaciones.estrella"), new Item.Properties().rarity(Rarity.RARE));

	// ---------------------------------------------------------------- núcleos

	public static final Item NUCLEO_2 = nucleo(2, Rarity.COMMON);
	public static final Item NUCLEO_3 = nucleo(3, Rarity.COMMON);
	public static final Item NUCLEO_4 = nucleo(4, Rarity.UNCOMMON);
	public static final Item NUCLEO_5 = nucleo(5, Rarity.RARE);
	public static final Item NUCLEO_6 = nucleo(6, Rarity.EPIC);
	public static final Item NUCLEO_7 = nucleo(7, Rarity.EPIC);

	// ---------------------------------------------------------------- herramientas por era

	public static final TagKey<Item> REPARA = TagKey.create(Registries.ITEM, Generaciones.id("repara_herramientas"));

	private static final ToolMaterial[] MATERIALES = {
			new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 600, 7.0f, 2.0f, 15, REPARA),
			new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1000, 8.5f, 3.0f, 15, REPARA),
			new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1600, 9.0f, 4.0f, 15, REPARA),
			new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2500, 13.0f, 5.0f, 15, REPARA),
			new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 16.0f, 7.0f, 15, REPARA),
	};

	/** PICOS[0] = nivel 1 … PICOS[4] = nivel 5. */
	public static final Item[] PICOS = new Item[5];
	public static final Item[] ESPADAS = new Item[5];

	static {
		for (int i = 0; i < 5; i++) {
			int nivel = i + 1;
			Item.Properties pico = new Item.Properties().pickaxe(MATERIALES[i], 1.0f, -2.8f);
			Item.Properties espada = new Item.Properties().sword(MATERIALES[i], 3.0f, -2.4f);
			if (nivel == 5) {
				pico.component(DataComponents.UNBREAKABLE, Unit.INSTANCE).fireResistant().rarity(Rarity.EPIC);
				espada.component(DataComponents.UNBREAKABLE, Unit.INSTANCE).fireResistant().rarity(Rarity.EPIC);
			} else if (nivel >= 3) {
				pico.rarity(Rarity.RARE);
				espada.rarity(Rarity.RARE);
			} else {
				pico.rarity(Rarity.UNCOMMON);
				espada.rarity(Rarity.UNCOMMON);
			}
			String[] picoLines = new String[nivel + 1];
			picoLines[0] = "tooltip.generaciones.herramienta.nivel" + nivel;
			for (int k = 1; k <= nivel; k++) {
				picoLines[k] = "tooltip.generaciones.pico.poder" + k;
			}
			PICOS[i] = item("pico_chispa_" + nivel, p -> new TooltipItem(p, picoLines), pico);
			ESPADAS[i] = item("espada_chispa_" + nivel, p -> new TooltipItem(p,
					"tooltip.generaciones.herramienta.nivel" + nivel, "tooltip.generaciones.espada.poder" + nivel), espada);
		}
	}

	// ---------------------------------------------------------------- máquinas

	public static final Block COLECTOR = machine("colector", ColectorEntity::new, () -> ModContent.COLECTOR_BE,
			"tooltip.generaciones.colector", "tooltip.generaciones.maquina.cofre");
	public static final Block MINERO = machine("minero", MineroEntity::new, () -> ModContent.MINERO_BE,
			"tooltip.generaciones.minero", "tooltip.generaciones.maquina.cofre");
	public static final Block GRANJA = machine("granja", GranjaEntity::new, () -> ModContent.GRANJA_BE,
			"tooltip.generaciones.granja", "tooltip.generaciones.maquina.cofre");

	public static final BlockEntityType<ColectorEntity> COLECTOR_BE = beType("colector", ColectorEntity::new, COLECTOR);
	public static final BlockEntityType<MineroEntity> MINERO_BE = beType("minero", MineroEntity::new, MINERO);
	public static final BlockEntityType<GranjaEntity> GRANJA_BE = beType("granja", GranjaEntity::new, GRANJA);

	// ---------------------------------------------------------------- jefe final

	public static final ResourceKey<net.minecraft.world.entity.EntityType<?>> ECLIPSE_KEY = ResourceKey.create(
			Registries.ENTITY_TYPE, Generaciones.id("eclipse"));
	public static final net.minecraft.world.entity.EntityType<com.generaciones.boss.Eclipse> ECLIPSE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE, ECLIPSE_KEY,
			net.minecraft.world.entity.EntityType.Builder.<com.generaciones.boss.Eclipse>of(com.generaciones.boss.Eclipse::new,
					net.minecraft.world.entity.MobCategory.MONSTER).sized(3.0f, 3.0f).fireImmune().clientTrackingRange(10)
					.build(ECLIPSE_KEY));

	// ---------------------------------------------------------------- armadura estelar

	public static final ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> ESTELAR_ASSET = ResourceKey.create(
			net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Generaciones.id("estelar"));
	public static final net.minecraft.world.item.equipment.ArmorMaterial ESTELAR = new net.minecraft.world.item.equipment.ArmorMaterial(
			50, java.util.Map.of(
					net.minecraft.world.item.equipment.ArmorType.BOOTS, 4,
					net.minecraft.world.item.equipment.ArmorType.LEGGINGS, 7,
					net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, 9,
					net.minecraft.world.item.equipment.ArmorType.HELMET, 4,
					net.minecraft.world.item.equipment.ArmorType.BODY, 20),
			20, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0f, 0.2f, REPARA, ESTELAR_ASSET);
	public static final Item CASCO_ESTELAR = armadura("casco_estelar", net.minecraft.world.item.equipment.ArmorType.HELMET);
	public static final Item PETO_ESTELAR = armadura("peto_estelar", net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
	public static final Item GREBAS_ESTELARES = armadura("grebas_estelares", net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
	public static final Item BOTAS_ESTELARES = armadura("botas_estelares", net.minecraft.world.item.equipment.ArmorType.BOOTS);

	private static Item armadura(String name, net.minecraft.world.item.equipment.ArmorType type) {
		return item(name, p -> new TooltipItem(p, "tooltip.generaciones.armadura", "tooltip.generaciones.armadura.set"),
				new Item.Properties().humanoidArmor(ESTELAR, type).rarity(Rarity.EPIC).fireResistant());
	}

	/** ¿Lleva puesta la armadura estelar completa? */
	public static boolean armaduraCompleta(net.minecraft.world.entity.LivingEntity e) {
		return e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(CASCO_ESTELAR)
				&& e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(PETO_ESTELAR)
				&& e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(GREBAS_ESTELARES)
				&& e.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(BOTAS_ESTELARES);
	}

	public static final Item CORAZON_ESTELAR = item("corazon_estelar", CorazonItem::new,
			new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant());

	private static Block machine(String name, java.util.function.BiFunction<BlockPos, BlockState, MachineEntity> factory,
			java.util.function.Supplier<BlockEntityType<? extends MachineEntity>> type, String... tooltip) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Generaciones.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, new MachineBlock(BlockBehaviour.Properties.of()
				.setId(key).strength(3.0f, 6.0f).sound(SoundType.METAL).lightLevel(st -> 6), factory, type));
		item(name, p -> new TooltipBlockItem(block, p, tooltip), new Item.Properties().useBlockDescriptionPrefix());
		return block;
	}

	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> beType(
			String name, FabricBlockEntityTypeBuilder.Factory<T> factory, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Generaciones.id(name),
				FabricBlockEntityTypeBuilder.create(factory, block).build());
	}

	/** Nivel del pico de chispa en la mano (0 si no es uno). */
	public static int picoLevel(ItemStack stack) {
		for (int i = 0; i < PICOS.length; i++) {
			if (stack.is(PICOS[i])) {
				return i + 1;
			}
		}
		return 0;
	}

	// ---------------------------------------------------------------- pestaña creativa

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
			BuiltInRegistries.CREATIVE_MODE_TAB.key(), Generaciones.id("generaciones"));
	public static final CreativeModeTab TAB = FabricCreativeModeTab.builder()
			.icon(() -> new ItemStack(GENERADOR_ITEM))
			.title(Component.translatable("itemGroup.generaciones"))
			.displayItems((params, output) -> {
				for (int gen = 1; gen <= Balance.MAX_GEN; gen++) {
					output.accept(generadorStack(gen));
				}
				output.accept(CHISPA);
				output.accept(PAQUETE_CHISPAS);
				output.accept(CRISTAL_CHISPAS);
				output.accept(ESTRELLA_CHISPAS);
				output.accept(NUCLEO_2);
				output.accept(NUCLEO_3);
				output.accept(NUCLEO_4);
				output.accept(NUCLEO_5);
				output.accept(NUCLEO_6);
				output.accept(NUCLEO_7);
				output.accept(COLECTOR);
				output.accept(MINERO);
				output.accept(GRANJA);
				output.accept(CORAZON_ESTELAR);
				output.accept(CASCO_ESTELAR);
				output.accept(PETO_ESTELAR);
				output.accept(GREBAS_ESTELARES);
				output.accept(BOTAS_ESTELARES);
				for (Item it : PICOS) output.accept(it);
				for (Item it : ESPADAS) output.accept(it);
			})
			.build();

	public static ItemStack generadorStack(int gen) {
		ItemStack stack = new ItemStack(GENERADOR_ITEM);
		if (gen > 1) {
			stack.set(DataComponents.BLOCK_STATE,
					BlockItemStateProperties.EMPTY.with(GeneradorBlock.GEN, gen));
		}
		return stack;
	}

	// ---------------------------------------------------------------- ayudantes

	private static Item nucleo(int gen, Rarity rarity) {
		return item("nucleo_" + gen, p -> new NucleoItem(p, gen), new Item.Properties().rarity(rarity));
	}

	private static Item item(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
		Identifier id = Generaciones.id(name);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
	}

	static void init() {
		net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ECLIPSE,
				com.generaciones.boss.Eclipse.createAttributes());
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);
	}

	private ModContent() {
	}
}
