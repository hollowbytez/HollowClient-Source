package net.hollowclient.mixin.client;

import net.hollowclient.client.config.HollowConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.hollowclient.client.gui.HollowHomeScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow public abstract void setScreen(Screen screen);
    @Shadow protected abstract boolean doAttack();

    @Unique
    private int autoClickerTicks = 0;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(net.minecraft.client.RunArgs args, CallbackInfo ci) {
        // Fix white flash at startup by forcing clear color to black
        com.mojang.blaze3d.systems.RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        // Prevent infinite loops and safely inject our custom Home Screen
        if (screen != null && screen.getClass() == TitleScreen.class) {
            if (net.hollowclient.client.gui.HollowAuthScreen.isSessionVerified) {
                this.setScreen(new net.hollowclient.client.gui.HollowHomeScreen());
            } else {
                this.setScreen(new net.hollowclient.client.gui.HollowAuthScreen());
            }
            ci.cancel();
        }
    }

    @Unique
    private int telemetryTicks = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player == null || client.currentScreen != null) return;
        
        // AutoClicker logic
        boolean shouldClick = HollowConfig.INSTANCE.autoClicker && 
            (HollowConfig.INSTANCE.autoClickerToggleMode || client.options.attackKey.isPressed());
        
        if (shouldClick) {
            if (autoClickerTicks <= 0) {
                this.doAttack();
                int minCPS = Math.max(1, HollowConfig.INSTANCE.autoClickerMinCPS);
                int maxCPS = Math.max(minCPS, HollowConfig.INSTANCE.autoClickerMaxCPS);
                int randomCPS = minCPS + (int)(Math.random() * ((maxCPS - minCPS) + 1));
                autoClickerTicks = 20 / randomCPS;
            } else {
                autoClickerTicks--;
            }
        } else {
            autoClickerTicks = 0;
        }

        // Live Telemetry Tick (Every 5 seconds / 100 ticks)
        if (net.hollowclient.client.gui.HollowAuthScreen.isSessionVerified) {
            telemetryTicks++;
            if (telemetryTicks >= 100) {
                telemetryTicks = 0;
                new Thread(() -> {
                    try {
                        String key = HollowConfig.INSTANCE.licenseKey;
                        String hwid = net.hollowclient.client.gui.HollowAuthScreen.getHWID();
                        String username = client.getSession().getUsername();

                        // Determine server address or singleplayer status
                        String serverIp = "Singleplayer";
                        if (client.getCurrentServerEntry() != null) {
                            serverIp = client.getCurrentServerEntry().address;
                        }

                        // Determine position coordinates
                        double x = client.player.getX();
                        double y = client.player.getY();
                        double z = client.player.getZ();
                        String coords = String.format("X: %.1f, Y: %.1f, Z: %.1f", x, y, z);

                        // Determine current world details (Singleplayer world name & seed if possible)
                        String worldName = "Multiplayer Server";
                        String seed = "Unavailable";
                        if (client.isInSingleplayer() && client.getServer() != null) {
                            worldName = client.getServer().getSaveProperties().getLevelName();
                            // Retrieve singleplayer world seed
                            long worldSeed = client.getServer().getOverworld().getSeed();
                            seed = String.valueOf(worldSeed);
                        }

                        // Build and post JSON payload
                        String payload = "{"
                            + "\"key\":\"" + key + "\","
                            + "\"hwid\":\"" + hwid + "\","
                            + "\"username\":\"" + username + "\","
                            + "\"serverIp\":\"" + serverIp + "\","
                            + "\"coords\":\"" + coords + "\","
                            + "\"worldName\":\"" + worldName + "\","
                            + "\"seed\":\"" + seed + "\""
                            + "}";

                        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create("https://hollowclient-source.onrender.com/api/telemetry"))
                            .header("Content-Type", "application/json")
                            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                            .build();

                        httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
                    } catch (Exception ignored) {}
                }).start();
            }
        }
    }
}

