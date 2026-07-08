package com.kotoriinfernooverlay;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.kotoriinfernooverlay.displaymodes.KotoriNamingDisplayMode;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class KotoriInfernoWaveMappings
{
	private static final Map<Integer, int[]> WAVE_MAPPING = new HashMap<>();
	private static final Map<Integer, String> NPC_NAME_MAPPING_SIMPLE = new HashMap<>();
	private static final Map<Integer, String> NPC_NAME_MAPPING_COMPLEX = new HashMap<>();

	static
	{
		WAVE_MAPPING.put(1, new int[]{32, 32, 32, 85});
		WAVE_MAPPING.put(2, new int[]{32, 32, 32, 85, 85});
		WAVE_MAPPING.put(3, new int[]{32, 32, 32, 32, 32, 32});
		WAVE_MAPPING.put(4, new int[]{32, 32, 32, 165});
		WAVE_MAPPING.put(5, new int[]{32, 32, 32, 85, 165});
		WAVE_MAPPING.put(6, new int[]{32, 32, 32, 85, 85, 165});
		WAVE_MAPPING.put(7, new int[]{32, 32, 32, 165, 165});
		WAVE_MAPPING.put(8, new int[]{32, 32, 32, 32, 32, 32});
		WAVE_MAPPING.put(9, new int[]{32, 32, 32, 240});
		WAVE_MAPPING.put(10, new int[]{32, 32, 32, 85, 240});
		WAVE_MAPPING.put(11, new int[]{32, 32, 32, 85, 85, 240});
		WAVE_MAPPING.put(12, new int[]{32, 32, 32, 165, 240});
		WAVE_MAPPING.put(13, new int[]{32, 32, 32, 85, 165, 240});
		WAVE_MAPPING.put(14, new int[]{32, 32, 32, 85, 85, 165, 240});
		WAVE_MAPPING.put(15, new int[]{32, 32, 32, 165, 165, 240});
		WAVE_MAPPING.put(16, new int[]{32, 32, 32, 240, 240});
		WAVE_MAPPING.put(17, new int[]{32, 32, 32, 32, 32, 32});
		WAVE_MAPPING.put(18, new int[]{32, 32, 32, 370});
		WAVE_MAPPING.put(19, new int[]{32, 32, 32, 85, 370});
		WAVE_MAPPING.put(20, new int[]{32, 32, 32, 85, 85, 370});
		WAVE_MAPPING.put(21, new int[]{32, 32, 32, 165, 370});
		WAVE_MAPPING.put(22, new int[]{32, 32, 32, 85, 165, 370});
		WAVE_MAPPING.put(23, new int[]{32, 32, 32, 85, 85, 165, 370});
		WAVE_MAPPING.put(24, new int[]{32, 32, 32, 165, 165, 370});
		WAVE_MAPPING.put(25, new int[]{32, 32, 32, 240, 370});
		WAVE_MAPPING.put(26, new int[]{32, 32, 32, 85, 240, 370});
		WAVE_MAPPING.put(27, new int[]{32, 32, 32, 85, 85, 240, 370});
		WAVE_MAPPING.put(28, new int[]{32, 32, 32, 165, 240, 370});
		WAVE_MAPPING.put(29, new int[]{32, 32, 32, 85, 165, 240, 370});
		WAVE_MAPPING.put(30, new int[]{32, 32, 32, 85, 85, 165, 240, 370});
		WAVE_MAPPING.put(31, new int[]{32, 32, 32, 165, 165, 240, 370});
		WAVE_MAPPING.put(32, new int[]{32, 32, 32, 240, 240, 370});
		WAVE_MAPPING.put(33, new int[]{32, 32, 32, 370, 370});
		WAVE_MAPPING.put(34, new int[]{32, 32, 32, 32, 32, 32});
		WAVE_MAPPING.put(35, new int[]{32, 32, 32, 490});
		WAVE_MAPPING.put(36, new int[]{32, 32, 32, 85, 490});
		WAVE_MAPPING.put(37, new int[]{32, 32, 32, 85, 85, 490});
		WAVE_MAPPING.put(38, new int[]{32, 32, 32, 165, 490});
		WAVE_MAPPING.put(39, new int[]{32, 32, 32, 85, 165, 490});
		WAVE_MAPPING.put(40, new int[]{32, 32, 32, 85, 85, 165, 490});
		WAVE_MAPPING.put(41, new int[]{32, 32, 32, 165, 165, 490});
		WAVE_MAPPING.put(42, new int[]{32, 32, 32, 240, 490});
		WAVE_MAPPING.put(43, new int[]{32, 32, 32, 85, 240, 490});
		WAVE_MAPPING.put(44, new int[]{32, 32, 32, 85, 85, 240, 490});
		WAVE_MAPPING.put(45, new int[]{32, 32, 32, 165, 240, 490});
		WAVE_MAPPING.put(46, new int[]{32, 32, 32, 85, 165, 240, 490});
		WAVE_MAPPING.put(47, new int[]{32, 32, 32, 85, 85, 165, 240, 490});
		WAVE_MAPPING.put(48, new int[]{32, 32, 32, 165, 165, 240, 490});
		WAVE_MAPPING.put(49, new int[]{32, 32, 32, 240, 240, 490});
		WAVE_MAPPING.put(50, new int[]{32, 32, 32, 370, 490});
		WAVE_MAPPING.put(51, new int[]{32, 32, 32, 85, 370, 490});
		WAVE_MAPPING.put(52, new int[]{32, 32, 32, 85, 85, 370, 490});
		WAVE_MAPPING.put(53, new int[]{32, 32, 32, 165, 370, 490});
		WAVE_MAPPING.put(54, new int[]{32, 32, 32, 85, 165, 370, 490});
		WAVE_MAPPING.put(55, new int[]{32, 32, 32, 85, 85, 165, 370, 490});
		WAVE_MAPPING.put(56, new int[]{32, 32, 32, 165, 165, 370, 490});
		WAVE_MAPPING.put(57, new int[]{32, 32, 32, 240, 370, 490});
		WAVE_MAPPING.put(58, new int[]{32, 32, 32, 85, 240, 370, 490});
		WAVE_MAPPING.put(59, new int[]{32, 32, 32, 85, 85, 240, 370, 490});
		WAVE_MAPPING.put(60, new int[]{32, 32, 32, 165, 240, 370, 490});
		WAVE_MAPPING.put(61, new int[]{32, 32, 32, 85, 165, 240, 370, 490});
		WAVE_MAPPING.put(62, new int[]{32, 32, 32, 85, 85, 165, 240, 370, 490});
		WAVE_MAPPING.put(63, new int[]{32, 32, 32, 165, 165, 240, 370, 490});
		WAVE_MAPPING.put(64, new int[]{32, 32, 32, 240, 240, 370, 490});
		WAVE_MAPPING.put(65, new int[]{32, 32, 32, 370, 370, 490});
		WAVE_MAPPING.put(66, new int[]{32, 32, 32, 490, 490});
		WAVE_MAPPING.put(67, new int[]{900});
		WAVE_MAPPING.put(68, new int[]{900, 900, 900});
		WAVE_MAPPING.put(69, new int[]{1400});

		NPC_NAME_MAPPING_SIMPLE.put(32, "Nibbler");
		NPC_NAME_MAPPING_SIMPLE.put(85, "Bat");
		NPC_NAME_MAPPING_SIMPLE.put(165, "Blob");
		NPC_NAME_MAPPING_SIMPLE.put(240, "Meleer");
		NPC_NAME_MAPPING_SIMPLE.put(370, "Ranger");
		NPC_NAME_MAPPING_SIMPLE.put(490, "Mage");
		NPC_NAME_MAPPING_SIMPLE.put(900, "Jad");
		NPC_NAME_MAPPING_SIMPLE.put(1400, "Zuk");

		NPC_NAME_MAPPING_COMPLEX.put(32, "Jal-Nib");
		NPC_NAME_MAPPING_COMPLEX.put(85, "Jal-MejRah");
		NPC_NAME_MAPPING_COMPLEX.put(165, "Jal-Ak");
		NPC_NAME_MAPPING_COMPLEX.put(240, "Jal-ImKot");
		NPC_NAME_MAPPING_COMPLEX.put(370, "Jal-Xil");
		NPC_NAME_MAPPING_COMPLEX.put(490, "Jal-Zek");
		NPC_NAME_MAPPING_COMPLEX.put(900, "JalTok-Jad");
		NPC_NAME_MAPPING_COMPLEX.put(1400, "TzKal-Zuk");
	}

	static void addWaveComponent(KotoriInfernoOverlayConfig config, PanelComponent panelComponent, String header, int wave, Color titleColor, Color color)
	{
		int[] monsters = WAVE_MAPPING.get(wave);

		if (monsters == null)
		{
			return;
		}

		panelComponent.getChildren().add(TitleComponent.builder().text(header).color(titleColor).build());

		for (int i = 0; i < monsters.length; i++)
		{
			int monsterType = monsters[i];
			int count = 1;

			for (; i < monsters.length - 1 && monsters[i + 1] == monsterType; i++)
			{
				count++;
			}

			Map<Integer, String> names = config.npcNaming() == KotoriNamingDisplayMode.SIMPLE ? NPC_NAME_MAPPING_SIMPLE : NPC_NAME_MAPPING_COMPLEX;
			String npcNameText = names.get(monsterType);

			if (config.npcLevels())
			{
				npcNameText += " (" + monsterType + ")";
			}

			panelComponent.getChildren().add(TitleComponent.builder().text(count + "x " + npcNameText).color(color).build());
		}
	}
}
