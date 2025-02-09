package de.dafuqs.spectrum.recipe.titration_barrel.dynamic;

import de.dafuqs.spectrum.SpectrumCommon;
import de.dafuqs.spectrum.helpers.InventoryHelper;
import de.dafuqs.spectrum.helpers.Support;
import de.dafuqs.spectrum.helpers.TimeHelper;
import de.dafuqs.spectrum.items.beverages.JadeWineItem;
import de.dafuqs.spectrum.recipe.titration_barrel.TitrationBarrelRecipe;
import de.dafuqs.spectrum.registries.SpectrumItems;
import de.dafuqs.spectrum.registries.SpectrumStatusEffects;
import net.id.incubus_core.recipe.IngredientStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class JadeWineRecipe extends TitrationBarrelRecipe {
	
	public static final RecipeSerializer<JadeWineRecipe> SERIALIZER = new SpecialRecipeSerializer<>(JadeWineRecipe::new);
	public static final Identifier UNLOCK_IDENTIFIER = SpectrumCommon.locate("progression/unlock_jade_wine");
	
	public static final int MIN_FERMENTATION_TIME_HOURS = 24;
	public static final ItemStack OUTPUT_STACK = SpectrumItems.JADE_WINE.getDefaultStack();
	public static final Item TAPPING_ITEM = Items.GLASS_BOTTLE;
	public static final List<IngredientStack> INGREDIENT_STACKS = new ArrayList<>() {{
		add(IngredientStack.of(Ingredient.ofItems(SpectrumItems.GERMINATED_JADE_VINE_SEEDS)));
		add(IngredientStack.of(Ingredient.ofItems(SpectrumItems.JADE_VINE_PETALS)));
	}};
	
	public JadeWineRecipe(Identifier identifier) {
		super(identifier, "", INGREDIENT_STACKS, OUTPUT_STACK, TAPPING_ITEM, MIN_FERMENTATION_TIME_HOURS, new TitrationBarrelRecipe.FermentationData(0.08F, List.of()), UNLOCK_IDENTIFIER);
	}
	
	@Override
	public ItemStack getOutput() {
		return tapWith(1, 1, false, 1.0F, this.minFermentationTimeHours * 60L * 60L, 0.8F, 0.4F); // downfall & temperature are for plains
	}
	
	@Override
	public ItemStack tap(Inventory inventory, int waterBuckets, long secondsFermented, float downfall, float temperature) {
		int bulbCount = InventoryHelper.getItemCountInInventory(inventory, SpectrumItems.GERMINATED_JADE_VINE_SEEDS);
		int petalCount = InventoryHelper.getItemCountInInventory(inventory, SpectrumItems.JADE_VINE_PETALS);
		boolean nectar = InventoryHelper.getItemCountInInventory(inventory, SpectrumItems.MOONSTRUCK_NECTAR) > 0;
		
		float thickness = getThickness(bulbCount, petalCount, waterBuckets);
		return tapWith(bulbCount, petalCount, nectar, thickness, secondsFermented, downfall, temperature);
	}
	
	public ItemStack tapWith(int bulbCount, int petalCount, boolean nectar, float thickness, long secondsFermented, float downfall, float temperature) {
		if(secondsFermented / 60 / 60 < this.minFermentationTimeHours) {
			return NOT_FERMENTED_LONG_ENOUGH_OUTPUT_STACK;
		}
		
		double bloominess = getBloominess(bulbCount, petalCount);
		float ageIngameDays = TimeHelper.minecraftDaysFromSeconds(secondsFermented);
		double alcPercent = getAlcPercentWithBloominess(ageIngameDays, downfall, bloominess, thickness);
		if(alcPercent >= 100) {
			return PURE_ALCOHOL_STACK;
		} else {
			List<StatusEffectInstance> effects = new ArrayList<>();
			
			int effectDuration = 1500;
			if(alcPercent >= 80) { effects.add(new StatusEffectInstance(StatusEffects.RESISTANCE, effectDuration)); effectDuration *= 2; }
			if(alcPercent >= 60) { effects.add(new StatusEffectInstance(StatusEffects.HASTE, effectDuration)); effectDuration *= 2; }
			if(alcPercent >= 40) { effects.add(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, effectDuration)); effectDuration *= 2; }
			if(alcPercent >= 20) { effects.add(new StatusEffectInstance(SpectrumStatusEffects.NOURISHING, effectDuration)); effectDuration *= 2; }
			if(nectar) { effects.add(new StatusEffectInstance(SpectrumStatusEffects.IMMUNITY, effectDuration)); }
			
			int nectarMod = nectar ? 3 : 1;
			effectDuration = 1500;
			int alcAfterBloominess = (int) (alcPercent / (nectarMod + bloominess ));
			if(alcAfterBloominess >= 40) { effects.add(new StatusEffectInstance(StatusEffects.BLINDNESS, effectDuration)); effectDuration *= 2; }
			if(alcAfterBloominess >= 30) { effects.add(new StatusEffectInstance(StatusEffects.POISON, effectDuration)); effectDuration *= 2; }
			if(alcAfterBloominess >= 20) { effects.add(new StatusEffectInstance(StatusEffects.NAUSEA, effectDuration)); effectDuration *= 2; }
			if(alcAfterBloominess >= 10) { effects.add(new StatusEffectInstance(StatusEffects.WEAKNESS, effectDuration)); }
			
			return new JadeWineItem.JadeWineBeverageProperties((long) ageIngameDays, (int) alcPercent, thickness, (float) bloominess, nectar, effects).getStack();
		}
	}
	
	// bloominess reduces the possibility of negative effects to trigger (better on the tongue)
	// but also reduces the potency of positive effects a bit
	protected static double getBloominess(int bulbCount, int petalCount) {
		if(bulbCount == 0) {
			return 0;
		}
		return (double) petalCount / (double) bulbCount / 2F;
	}
	
	// the amount of solid to liquid
	// adding more water will increase the amount of bottles the player can harvest from the barrel
	// but too much water will - who would have thought - water it down
	protected float getThickness(int bulbCount, int petalCount, int waterCount) {
		if(waterCount == 0) {
			return 0;
		}
		return waterCount / (bulbCount + petalCount / 8F);
	}
	
	// the alc % determines the power of effects when drunk
	// it generally increases the longer the wine has fermented
	//
	// another detail: the more rainy the weather (downfall) the more water evaporates
	// in contrast to alcohol, making the drink stronger / weaker in return
	private double getAlcPercentWithBloominess(float ageIngameDays, float downfall, double bloominess, double thickness) {
		return Support.logBase(1 + this.fermentationData.fermentationSpeedMod(), ageIngameDays * (0.5 + thickness / 2) * (0.5D + downfall / 2D)) - bloominess;
	}
	
	@Override
	public boolean matches(Inventory inventory, World world) {
		boolean bulbsFound = false;
		
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if(stack.isEmpty()) {
				continue;
			}
			if(stack.isOf(SpectrumItems.GERMINATED_JADE_VINE_SEEDS)) {
				bulbsFound = true;
			} else if(!stack.isOf(SpectrumItems.JADE_VINE_PETALS) && !stack.isOf(SpectrumItems.MOONSTRUCK_NECTAR)) {
				return false;
			}
		}
		
		return bulbsFound;
	}
	
	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
	
}
