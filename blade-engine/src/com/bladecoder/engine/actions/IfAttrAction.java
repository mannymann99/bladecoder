/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engine.actions;

import com.bladecoder.engine.model.AnimationRenderer;
import com.bladecoder.engine.model.BaseActor;
import com.bladecoder.engine.model.InteractiveActor;
import com.bladecoder.engine.model.Inventory;
import com.bladecoder.engine.model.Scene;
import com.bladecoder.engine.model.SpriteActor;
import com.bladecoder.engine.model.VerbRunner;
import com.bladecoder.engine.model.World;
import com.bladecoder.engine.util.ActionUtils;
import com.bladecoder.engine.util.EngineLogger;

@ActionDescription(name = "IfActorAttr", value = "Execute the actions inside the If/EndIf if the attribute has the specified value.")
public class IfAttrAction extends AbstractIfAction {
	public static final String ENDTYPE_VALUE = "else";

	public enum ActorAttribute {
		STATE, VISIBLE, INTERACTIVE, IN_INVENTORY, TARGET, IN_SCENE, LAYER, DIRECTION, IN_UI, INSIDE
	}

	@ActionProperty(required = true)
	@ActionPropertyDescription("The target actor")
	private SceneActorRef actor;

	@ActionProperty(required = true, defaultValue = "STATE")
	@ActionPropertyDescription("The actor attribute")
	private ActorAttribute attr;

	@ActionProperty
	@ActionPropertyDescription("The attribute value")
	private String value;

	private World w;

	@Override
	public void init(World w) {
		this.w = w;
	}

	@Override
	public boolean run(VerbRunner cb) {
		Scene s = actor.getScene(w);

		final String actorId = actor.getActorId();
		if (actorId == null) {
			// if called inside a scene verb and no actor is specified, return
			EngineLogger.error(getClass() + ": No actor specified");
			return false;
		}

		BaseActor a = s.getActor(actorId, true);

		if (attr.equals(ActorAttribute.STATE) && a instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor) a;
			if (!ActionUtils.compareNullStr(value, ia.getState())) {
				gotoElse(cb);
			}
		} else if (attr.equals(ActorAttribute.VISIBLE)) {
			boolean val = Boolean.parseBoolean(value);
			if (val != a.isVisible()) {
				gotoElse(cb);
			}
		} else if (attr.equals(ActorAttribute.INTERACTIVE)) {
			boolean val = Boolean.parseBoolean(value);

			if (a instanceof InteractiveActor) {
				if (val != ((InteractiveActor) a).getInteraction()) {
					gotoElse(cb);
				}
			} else if (val == true) {
				gotoElse(cb);
			}
		} else if (attr.equals(ActorAttribute.IN_INVENTORY)) {
			// 'value' can have the inventory name to search in
			// or 'true/false' to search in the current inventory.

			Inventory inventory = null;
			boolean val = true;

			if (value != null) {
				inventory = w.getInventories().get(value);
			}

			if (inventory == null) {
				// boolean mode: search in the current inventory
				val = Boolean.parseBoolean(value);
				inventory = w.getInventory();
			}

			SpriteActor item = null;

			if (a != null)
				item = inventory.get(a.getId());

			if ((val && item == null) || (!val && item != null)) {
				gotoElse(cb);
			}
		} else if (attr.equals(ActorAttribute.TARGET)) {

			if (!ActionUtils.compareNullStr(value, cb.getCurrentTarget())) {
				gotoElse(cb);
			}
		} else if (attr.equals(ActorAttribute.IN_SCENE)) {
			boolean val = Boolean.parseBoolean(value);

			BaseActor a2 = s.getActor(actorId, false);

			if ((val && a2 == null) || (!val && a2 != null))
				gotoElse(cb);
		} else if (attr.equals(ActorAttribute.IN_UI)) {
			boolean val = Boolean.parseBoolean(value);

			BaseActor a2 = w.getUIActors().get(actorId);

			if ((val && a2 == null) || (!val && a2 != null))
				gotoElse(cb);
		} else if (attr.equals(ActorAttribute.LAYER) && a instanceof InteractiveActor) {
			InteractiveActor ia = (InteractiveActor) a;
			if (!ActionUtils.compareNullStr(value, ia.getLayer())) {
				gotoElse(cb);
			}
		} else if (attr.equals(ActorAttribute.DIRECTION) && a instanceof SpriteActor) {
			SpriteActor sa = (SpriteActor) a;

			if (sa.getRenderer() instanceof AnimationRenderer) {
				String dir = null;

				String anim = ((AnimationRenderer) sa.getRenderer()).getCurrentAnimationId();
				int idx = anim.lastIndexOf('.');

				if (idx != -1)
					dir = anim.substring(idx + 1);

				if (!ActionUtils.compareNullStr(value, dir)) {
					gotoElse(cb);
				}
			}
		} else if (attr.equals(ActorAttribute.INSIDE)) {
			BaseActor insideActor = w.getCurrentScene().getActor(value, false);
			boolean inside = false;

			if (a != null && insideActor != null)
				inside = insideActor.getBBox().contains(a.getX(), a.getY());
			else
				EngineLogger.debug("Actor for inside test not found: " + value);

			if (!inside) {
				gotoElse(cb);
			}
		}

		return false;
	}
}
