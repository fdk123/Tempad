package me.codexadrian.tempad.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Chaptered, scrollable 1.20.1 guide shared by the projector and Tempad upgrade. */
public final class KnowledgeRepositoryScreen extends Screen {
    private record GuidePage(String titleKey, String bodyKey) {
        static GuidePage item(String id) { return new GuidePage("item.tempad." + id, "guide.tempad.entry." + id); }
    }

    private static final List<GuidePage> OVERVIEW = java.util.stream.IntStream.range(0, 10)
        .mapToObj(index -> new GuidePage("guide.tempad.page." + index + ".title", "guide.tempad.page." + index + ".body"))
        .toList();
    private static final List<GuidePage> IRON = List.of(
        GuidePage.item("chronon_cell"), GuidePage.item("chronon_generator"), GuidePage.item("location_card"),
        GuidePage.item("card_wallet"), GuidePage.item("timedoor_marker"), GuidePage.item("timedoor_projector"),
        GuidePage.item("location_broadcaster"));
    private static final List<GuidePage> STEEL = List.of(
        GuidePage.item("time_steel"), GuidePage.item("chronon_battery"), GuidePage.item("chronometer"),
        GuidePage.item("chronomark"), GuidePage.item("tempad"),
        new GuidePage("block.tempad.workstation", "guide.tempad.entry.workstation"),
        GuidePage.item("time_twister"), GuidePage.item("screening_device"), GuidePage.item("metronome"));
    private static final List<List<GuidePage>> CHAPTERS = List.of(OVERVIEW, IRON, STEEL);
    private static final String[] CHAPTER_KEYS = {
        "guide.tempad.chapter.overview", "guide.tempad.chapter.iron", "guide.tempad.chapter.steel"
    };

    @Nullable private final Screen parent;
    private final List<Button> topicButtons = new ArrayList<>();
    private final Button[] chapterButtons = new Button[CHAPTERS.size()];
    private Button previous;
    private Button next;
    private int chapter;
    private int topic;
    private int listScroll;
    private int textScroll;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;
    private int visibleRows;

    public KnowledgeRepositoryScreen(@Nullable Screen parent) {
        super(Component.translatable("guide.tempad.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        topicButtons.clear();
        panelWidth = Math.min(520, width - 16);
        panelHeight = Math.min(300, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        listWidth = Math.min(170, Math.max(96, panelWidth * 38 / 100));
        visibleRows = Math.max(1, (panelHeight - 108) / 21);
        int tabWidth = (panelWidth - 24) / CHAPTERS.size();
        for (int index = 0; index < CHAPTERS.size(); index++) {
            final int selected = index;
            chapterButtons[index] = addRenderableWidget(Button.builder(Component.translatable(CHAPTER_KEYS[index]),
                button -> selectChapter(selected)).bounds(panelX + 12 + index * tabWidth, panelY + 24, tabWidth - 4, 20).build());
        }
        for (int row = 0; row < visibleRows; row++) {
            final int selectedRow = row;
            topicButtons.add(addRenderableWidget(Button.builder(Component.empty(),
                button -> selectTopic(listScroll + selectedRow)).bounds(panelX + 12, panelY + 56 + row * 21,
                listWidth - 10, 20).build()));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> selectTopic(topic - 1))
            .bounds(panelX + listWidth + 22, panelY + panelHeight - 30, 24, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> selectTopic(topic + 1))
            .bounds(panelX + listWidth + 52, panelY + panelHeight - 30, 24, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
            .bounds(panelX + panelWidth - 91, panelY + panelHeight - 30, 80, 20).build());
        updateButtons();
    }

    private void selectChapter(int selected) {
        chapter = Math.max(0, Math.min(CHAPTERS.size() - 1, selected));
        topic = 0;
        listScroll = 0;
        textScroll = 0;
        updateButtons();
    }

    private void selectTopic(int selected) {
        List<GuidePage> pages = CHAPTERS.get(chapter);
        topic = Math.max(0, Math.min(pages.size() - 1, selected));
        if (topic < listScroll) listScroll = topic;
        if (topic >= listScroll + visibleRows) listScroll = topic - visibleRows + 1;
        textScroll = 0;
        updateButtons();
    }

    private void updateButtons() {
        List<GuidePage> pages = CHAPTERS.get(chapter);
        for (int index = 0; index < chapterButtons.length; index++) chapterButtons[index].active = index != chapter;
        for (int row = 0; row < topicButtons.size(); row++) {
            int index = listScroll + row;
            Button button = topicButtons.get(row);
            button.visible = index < pages.size();
            if (button.visible) {
                String label = Component.translatable(pages.get(index).titleKey()).getString();
                int availableWidth = listWidth - 20;
                if (font.width(label) > availableWidth)
                    label = font.plainSubstrByWidth(label, availableWidth - font.width("…")) + "…";
                button.setMessage(Component.literal(label));
                button.active = index != topic;
            }
        }
        previous.active = topic > 0;
        next.active = topic < pages.size() - 1;
    }

    private int contentX() { return panelX + listWidth + 20; }
    private int contentWidth() { return Math.max(60, panelWidth - listWidth - 34); }
    private int visibleTextLines() { return Math.max(1, (panelHeight - 120) / 11); }
    private List<FormattedCharSequence> bodyLines() {
        return font.split(Component.translatable(CHAPTERS.get(chapter).get(topic).bodyKey()), contentWidth() - 12);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < panelX || mouseX > panelX + panelWidth || mouseY < panelY || mouseY > panelY + panelHeight)
            return super.mouseScrolled(mouseX, mouseY, delta);
        int step = delta > 0 ? -1 : 1;
        if (mouseX < contentX()) {
            listScroll = Math.max(0, Math.min(Math.max(0, CHAPTERS.get(chapter).size() - visibleRows), listScroll + step));
            updateButtons();
        } else {
            textScroll = Math.max(0, Math.min(Math.max(0, bodyLines().size() - visibleTextLines()), textScroll + step));
        }
        return true;
    }

    @Override public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xEE191C24);
        graphics.fill(panelX + listWidth + 15, panelY + 52, panelX + listWidth + 16, panelY + panelHeight - 38, 0xFF6E6047);
        graphics.drawCenteredString(font, title, panelX + panelWidth / 2, panelY + 8, 0xFFEDBD69);
        GuidePage page = CHAPTERS.get(chapter).get(topic);
        graphics.drawString(font, Component.translatable(page.titleKey()), contentX(), panelY + 60, 0xFFFFFFFF);
        List<FormattedCharSequence> lines = bodyLines();
        textScroll = Math.min(textScroll, Math.max(0, lines.size() - visibleTextLines()));
        for (int line = 0; line < visibleTextLines() && textScroll + line < lines.size(); line++)
            graphics.drawString(font, lines.get(textScroll + line), contentX(), panelY + 81 + line * 11, 0xFFE0E0E0);
        if (textScroll > 0) graphics.drawString(font, "▲", panelX + panelWidth - 24, panelY + 60, 0xFFEDBD69);
        if (textScroll + visibleTextLines() < lines.size())
            graphics.drawString(font, "▼", panelX + panelWidth - 24, panelY + panelHeight - 49, 0xFFEDBD69);
        graphics.drawString(font, (topic + 1) + "/" + CHAPTERS.get(chapter).size(),
            panelX + listWidth + 85, panelY + panelHeight - 24, 0xFFAAAAAA);
        int lastVisible = Math.min(CHAPTERS.get(chapter).size(), listScroll + visibleRows);
        graphics.drawString(font, (listScroll + 1) + "-" + lastVisible + "/" + CHAPTERS.get(chapter).size(),
            panelX + 12, panelY + panelHeight - 48, 0xFFAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
