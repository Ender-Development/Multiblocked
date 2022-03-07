package io.github.cleanroommc.multiblocked.api.gui.widget.imp;

import io.github.cleanroommc.multiblocked.api.gui.texture.ColorBorderTexture;
import io.github.cleanroommc.multiblocked.api.gui.texture.ColorRectTexture;
import io.github.cleanroommc.multiblocked.api.gui.texture.GuiTextureGroup;
import io.github.cleanroommc.multiblocked.api.gui.texture.ResourceTexture;
import io.github.cleanroommc.multiblocked.api.gui.texture.TextTexture;
import io.github.cleanroommc.multiblocked.api.gui.util.FileNode;
import io.github.cleanroommc.multiblocked.api.gui.widget.Widget;
import io.github.cleanroommc.multiblocked.api.gui.widget.WidgetGroup;
import io.github.cleanroommc.multiblocked.util.Size;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class DialogWidget extends WidgetGroup {
    private static final int HEIGHT = 128;
    private static final int WIDTH = 184;
    public final WidgetGroup parent;
    protected Runnable onClosed;

    public DialogWidget(WidgetGroup parent, boolean isClient) {
        super(0, 0, parent.getSize().width, parent.getSize().height);
        this.parent = parent;
        if (isClient) setClientSideWidget();
        parent.addWidget(this);
    }

    public DialogWidget setOnClosed(Runnable onClosed) {
        this.onClosed = onClosed;
        return this;
    }

    public void close() {
        parent.waitToRemoved(this);
        if (onClosed != null) {
            onClosed.run();
        }
    }

    @Override
    public void drawInBackground(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableDepth();
        GlStateManager.translate(0, 0, 200);
        super.drawInBackground(mouseX, mouseY, partialTicks);
        GlStateManager.translate(0, 0, -200);
        GlStateManager.enableDepth();
    }

    @Override
    public Widget mouseClicked(int mouseX, int mouseY, int button) {
        Widget widget = super.mouseClicked(mouseX, mouseY, button);
        return widget == null ? this : widget;
    }

    @Override
    public Widget keyTyped(char charTyped, int keyCode) {
        Widget widget = super.keyTyped(charTyped, keyCode);
        if (widget == null && keyCode == 1) {
            close();
        }
        return widget == null ? this : widget;
    }

    public static DialogWidget showFileDialog(WidgetGroup parent, String title, File dir, boolean isSelector, Consumer<File> result) {
        Size size = parent.getSize();
        DialogWidget dialog = new DialogWidget(parent, true);
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                return dialog;
            }
        }
        dialog.addWidget(new ImageWidget(0, 0, parent.getSize().width, parent.getSize().height, new ColorRectTexture(0x4f000000)));
        AtomicReference<File> selected = new AtomicReference<>();
        selected.set(dir);
        dialog.addWidget(new TreeListWidget<>(0, 0, 130, size.height, new FileNode(dir), node -> selected.set(node.getKey()))
                .setNodeTexture(new ResourceTexture("multiblocked:textures/gui/bordered_background.png"))
                .canSelectNode(true)
                .setLeafTexture(new ResourceTexture("multiblocked:textures/gui/darkened_slot.png")));
        int x = 130 + (size.width - 133 - WIDTH) / 2;
        int y = (size.height - HEIGHT) / 2;
        dialog.addWidget(new ImageWidget(x, y, WIDTH, HEIGHT, new ResourceTexture("multiblocked:textures/gui/bordered_background.png")));
        dialog.addWidget(new ButtonWidget(x + WIDTH / 2 - 30 - 20, y + HEIGHT - 32, 40, 20, cd -> {
            dialog.close();
            if (result != null) result.accept(selected.get());
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/darkened_slot.png"), new TextTexture("confirm", -1).setDropShadow(true)).setHoverBorderTexture(1, 0xff000000));
        dialog.addWidget(new ButtonWidget(x + WIDTH / 2 + 30 - 20, y + HEIGHT - 32, 40, 20, cd -> {
            dialog.close();
            if (result != null) result.accept(null);
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/darkened_slot.png"), new TextTexture("cancel", 0xffff0000).setDropShadow(true)).setHoverBorderTexture(1, 0xff000000));
        if (isSelector) {
            dialog.addWidget(new ImageWidget(x + 8, y + HEIGHT / 2 - 5, WIDTH - 16, 20,
                    new GuiTextureGroup(new ColorBorderTexture(1, -1), new ColorRectTexture(0xff000000))));
            dialog.addWidget(new ImageWidget(x + 8, y + HEIGHT / 2 - 5, WIDTH - 16, 20,
                    new TextTexture("", -1).setWidth(WIDTH - 16).setType(TextTexture.TextType.ROLL)
                            .setSupplier(() -> {
                                if (selected.get() != null) {
                                    return selected.get().toString();
                                }
                                return "no file selected";
                            })));
        } else {
            dialog.addWidget(new TextFieldWidget(x + WIDTH / 2 - 38, y + HEIGHT / 2 - 10, 76, 20, true, ()->{
                File file = selected.get();
                if (file != null && !file.isDirectory()) {
                    return selected.get().getName();
                }
                return "";
            }, res->{
                File file = selected.get();
                if (file == null) return;
                if (file.isDirectory()) {
                    selected.set(new File(file, res));
                } else {
                    selected.set(new File(file.getParent(), res));
                }
            }));
        }
        dialog.addWidget(new ButtonWidget(x + 15, y + 15, 20, 20, cd -> {
            File file = selected.get();
            if (file != null) {
                try {
                    Desktop.getDesktop().open(file.isDirectory() ? file : file.getParentFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/darkened_slot.png"), new TextTexture("F", -1).setDropShadow(true)).setHoverBorderTexture(1, 0xff000000).setHoverTooltip("open folder"));
        dialog.addWidget(new ImageWidget(x + 15, y + 20, WIDTH - 30,10, new TextTexture(title, -1).setWidth(WIDTH - 30).setDropShadow(true)));
        //        dialog.addWidget(new LabelWidget(x + WIDTH / 2, y + 11, ()->title).setTextColor(-1));
        return dialog;
    }
}
