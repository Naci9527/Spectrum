package de.dafuqs.spectrum.recipe;

import de.dafuqs.spectrum.progression.UnlockToastManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface GatedRecipe extends Recipe<Inventory> {
	
	boolean canPlayerCraft(PlayerEntity playerEntity);
	
	Identifier getRequiredAdvancementIdentifier();
	
	default void registerInToastManager(RecipeType recipeType, GatedRecipe gatedRecipe) {
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
			registerInToastManagerClient(recipeType, gatedRecipe);
		}
	}
	
	@Environment(EnvType.CLIENT)
	private void registerInToastManagerClient(RecipeType recipeType, GatedRecipe gatedRecipe) {
		UnlockToastManager.registerGatedRecipe(recipeType, gatedRecipe);
	}
	
	TranslatableText getSingleUnlockToastString();
	
	TranslatableText getMultipleUnlockToastString();
	
	static void writeNullableIdentifier(PacketByteBuf buf, @Nullable Identifier identifier) {
		if(identifier == null) {
			buf.writeBoolean(false);
		} else {
			buf.writeBoolean(true);
			buf.writeIdentifier(identifier);
		}
	}
	
	static @Nullable Identifier readNullableIdentifier(PacketByteBuf buf) {
		boolean notNull = buf.readBoolean();
		if(notNull) {
			return buf.readIdentifier();
		}
		return null;
	}
	
}
