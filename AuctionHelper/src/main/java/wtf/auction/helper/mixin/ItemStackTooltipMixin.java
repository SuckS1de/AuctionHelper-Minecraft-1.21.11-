package wtf.auction.helper.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

	@Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
	private void appendAuctionUnitPrice(CallbackInfoReturnable<List<Text>> cir) {
		MinecraftClient mc = MinecraftClient.getInstance();

		if (!(mc.currentScreen instanceof GenericContainerScreen screen) || !isAuctionScreen(screen)) return;

		ItemStack stack = (ItemStack) (Object) this;
		if (stack.isEmpty() || stack.getCount() <= 1) return;

		List<Text> original = cir.getReturnValue();
		if (original == null) return;

		List<Text> tooltip = new ArrayList<>(original);

		int totalPrice = extractPriceFromTooltip(tooltip);
		if (totalPrice <= 0) return;

		int unitPrice = Math.max(1, totalPrice / Math.max(1, stack.getCount()));
		int insertAt = findPriceLineIndex(tooltip);

		String ip = getCurrentServerIP(mc).toLowerCase();
		Text unitLine;

		if (ip.contains("holyworld")) {
			return; //Не надо на холике есть
		}
		else if (ip.contains("funtime")) {
			unitLine = Text.literal("§2$ §fЗа 1 шт: §a" + formatPrice(unitPrice));
		}
		else if (ip.contains("luxorcraft")) {
			return; //есть тут
		}
		else if (ip.contains("chillzone")) {
			unitLine = Text.literal("  * §fЦена за 1 шт: §e" + formatPrice(unitPrice) + "⛃");
		}
		else {
			unitLine = Text.literal("§f| §fЗа 1 шт: §e" + formatPrice(unitPrice));
		}

		if (insertAt >= 0 && insertAt + 1 <= tooltip.size()) {
			tooltip.add(insertAt + 1, unitLine);
		} else {
			tooltip.add(unitLine);
		}

		cir.setReturnValue(tooltip);
	}

	private String getCurrentServerIP(MinecraftClient mc) {
		ServerInfo serverData = mc.getCurrentServerEntry();
		return (serverData != null) ? serverData.address : "local";
	}

	private static boolean isAuctionScreen(GenericContainerScreen screen) {
		String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
		return title.contains("Аукцион") ||
				title.contains("Аукционы") ||
				title.contains("Auction House") ||
				title.contains("Торговая площадка") ||
				title.contains("Аукционы") ||
				title.contains("Auctions") ||
				title.contains("Auction") ||
				title.contains("Аукціон") ||
				title.contains("Аукціони") ||
				title.contains("Поиск");
	}

	private static int extractPriceFromTooltip(List<Text> tooltip) {
		for (Text text : tooltip) {
			String line = text.getString();
			if (line == null) continue;
			String lower = line.toLowerCase();
			if (lower.contains("цена") || lower.contains("price") || line.contains("$") || line.contains("⛃")) {
				String numbers = line.replaceAll("[^0-9]", "");
				if (!numbers.isEmpty()) {
					try {
						return Integer.parseInt(numbers);
					} catch (Exception ignored) {}
				}
			}
		}
		return -1;
	}

	private static int findPriceLineIndex(List<Text> tooltip) {
		for (int i = 0; i < tooltip.size(); i++) {
			String line = tooltip.get(i).getString();
			if (line == null || line.isEmpty()) continue;
			String lower = line.toLowerCase();
			if (lower.contains("цена") || lower.contains("price") || line.contains("$") || line.contains("⛃")) {
				return i;
			}
		}
		return -1;
	}

	private static String formatPrice(int price) {
		String value = String.valueOf(Math.max(0, price));
		StringBuilder builder = new StringBuilder(value.length() + value.length() / 3);
		for (int i = 0; i < value.length(); i++) {
			if (i > 0 && (value.length() - i) % 3 == 0) {
				builder.append('.');
			}
			builder.append(value.charAt(i));
		}
		return builder.toString();
	}
}