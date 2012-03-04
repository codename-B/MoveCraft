package com.sycoprime.movecraft.listeners;

import com.sycoprime.movecraft.BlocksInfo;
import com.sycoprime.movecraft.Central;
import com.sycoprime.movecraft.Craft;
import com.sycoprime.movecraft.CraftMover;
import com.sycoprime.movecraft.CraftType;
import com.sycoprime.movecraft.Craft_Hyperspace;
import com.sycoprime.movecraft.DataBlock;
import com.sycoprime.movecraft.MoveCraft_Timer;
import com.sycoprime.movecraft.plugins.PermissionInterface;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PlayerListener implements Listener{
    
    	

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		Craft craft = Craft.getCraft(player);

		if (craft != null) {
			Craft.removeCraft(craft);
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();

		Craft craft = Craft.getCraft(player);

		if (craft != null) {
			//craft.setSpeed(1);

			if (craft.isIsOnBoard() && !craft.isOnCraft(player, false)) {
				player.sendMessage(ChatColor.YELLOW + "You get off the " + craft.name);
				player.sendMessage(ChatColor.GRAY + "Type /" + craft.name
						+ " remote for remote control");
				player.sendMessage(ChatColor.YELLOW + "If you don't, you'll lose control in 15 seconds.");
				craft.setIsOnBoard(false);
				craft.setHaveControl(false);

				int CraftReleaseDelay = 15;
				try {
					CraftReleaseDelay = Integer.parseInt(Central.configSetting("CraftReleaseDelay"));
				}
				catch (NumberFormatException ex) {
					System.out.println("ERROR with playermove. Could not parse " + Central.configSetting("CraftReleaseDelay"));
				}
				if(CraftReleaseDelay != 0)
					MoveCraft_Timer.playerTimers.put(player,
							new MoveCraft_Timer(CraftReleaseDelay, craft, "abandonCheck", false));
					//craft.timer = new MoveCraft_Timer(CraftReleaseDelay, craft, "abandonCheck", false);
			} else if (!craft.isIsOnBoard() && craft.isOnCraft(player, false)) {
				player.sendMessage(ChatColor.YELLOW + "Welcome on board");
				craft.setIsOnBoard(true);
				craft.setHaveControl(true);
				//if(craft.timer != null)
					//craft.timer.Destroy();
				MoveCraft_Timer timer = MoveCraft_Timer.playerTimers.get(player);
				if(timer != null)
					timer.Destroy();
			} else if(craft.type.listenMovement == true) {
				Location fromLoc = event.getFrom();
				Location toLoc = event.getTo();
				int dx = toLoc.getBlockX() - fromLoc.getBlockX();
				int dy = toLoc.getBlockY() - fromLoc.getBlockY();
				int dz = toLoc.getBlockZ() - fromLoc.getBlockZ();					

				CraftMover cm = new CraftMover(craft);
				cm.calculatedMove(dx, dy, dz);				
			}
		}
	}
        
        @EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		
		Craft playerCraft = Craft.getCraft(player);

		if(action == Action.RIGHT_CLICK_BLOCK) {
			
			if(event.hasBlock()) {
				Block block = event.getClickedBlock();
				
				Central.debugMessage("The action has a block " + block + " associated with it.", 4);
				
// Disabling signs until I have a handle on the group perms -- dlmarti
//				if(block.getTypeId() == 63 || block.getTypeId() == 68) {
//					MoveCraft_BlockListener.ClickedASign(player, block);
//					return;
//				}
				
				if(block.getTypeId() == 54 || 
						block.getTypeId() == 23 || 
						block.getTypeId() == 61 ) {
					//Need to handle workbench as well...
					
					return;
				}
				
				if("true".equals(Central.configSetting("RequireRemote")) && playerCraft != null) {
					playerCraft.addBlock(block);
				}

				/*
				Craft craft = Craft.getCraft(block.getX(),
						block.getY(), block.getZ());
				
				if(craft != null) {			
					System.out.println("Craft at that loc is " + craft.name + " of type " + craft.type.name);
					
					Block changedBlock = block.getFace(event.getBlockFace());
					Location blockLoc = changedBlock.getLocation();
					//if(Craft.getCraft(blockLoc.getBlockX(), blockLoc.getBlockX(), blockLoc.getBlockX()) == null) {
					int cX = ((int) (blockLoc.getX()) - craft.minX);
					int cY = ((int) (blockLoc.getY()) - craft.minY);
					int cZ = ((int) (blockLoc.getZ()) - craft.minZ);
					if(!craft.isCraftBlock(cX, cY, cZ)) {
						MoveCraft_BlockListener.updatedCraft = craft;
						return;
					}
				}
				*/
			}

			if (playerCraft != null) {
				if ("true".equals(Central.configSetting("RequireRemote")) && event.getItem().getTypeId() != playerCraft.type.remoteControllerItem){
					return;
                                }
                                
				playerUsedAnItem(player, playerCraft);
			} else {
				Vector pVel = player.getVelocity();
				if(player.getLocation().getPitch() < 90 || player.getLocation().getPitch() > 180)
					pVel.setX(pVel.getX() + 1);
				else
					pVel.setY(pVel.getY() + 1);
			}				
		}

		if(action == Action.RIGHT_CLICK_AIR && playerCraft != null && playerCraft.type.listenItem == true) {
			if ("true".equals(Central.configSetting("RequireRemote")) && 
					event.getItem().getTypeId() != playerCraft.type.remoteControllerItem)
				return;
			
			playerUsedAnItem(player, playerCraft);
		}
		
		/*
		if(action == Action.RIGHT_CLICK_AIR && playerCraft == null && MoveCraft.instance.DebugMode) {
			Vector pVel = player.getVelocity();
			int dx = 3;
			int dy = 0;
			int dz = 0;
			pVel = pVel.add(new Vector(dx, dy, dz));
			player.setVelocity(pVel);
		}
		*/
	}
	
	public void playerUsedAnItem(Player player, Craft craft) {
		// minimum time between 2 swings
		if (System.currentTimeMillis() - craft.lastMove < 0.2 * 1000)
			return;

		if (craft.blockCount <= 0) {
			Central.getPluginInstance().releaseCraft(player, craft);
			return;
		}

		ItemStack pItem = player.getItemInHand();
		int item = pItem.getTypeId();



		//MoveCraft.instance.DebugMessage(player.getName() + " used item " + Integer.toString(item));

		// the craft won't budge if you have any tool in the hand
		if (!craft.isHaveControl()) {
			if( (item == craft.type.remoteControllerItem || 
					item == Integer.parseInt(Central.configSetting("UniversalRemoteId")))
					&& !craft.isOnCraft(player, true)
					&& PermissionInterface.CheckPermission(player, "remote")) {
				if (craft.isHaveControl()) {
					player.sendMessage(ChatColor.YELLOW + "You switch off the remote controller");
				} else {
					MoveCraft_Timer timer = MoveCraft_Timer.playerTimers.get(player);
					if(timer != null)
						timer.Destroy();
					player.sendMessage(ChatColor.YELLOW + "You switch on the remote controller");
				}
				craft.setHaveControl(!craft.isHaveControl());
			}					
			else return;
		}

		if(Central.configSetting("RequireStick").equalsIgnoreCase("true") && item != 280)
			return;

		// minimum time between 2 swings
		if (System.currentTimeMillis() - craft.lastMove < 0.2 * 1000)
			return;

		/*
				int dx = 0, dy = 0, dz = 0;
				float rotation = player.getLocation().getYaw();

				if(rotation > 45 && rotation < 135)
					rotation = 90;
				else if(rotation > 135 && rotation < 225)
					rotation = 180;
				else if (rotation > 225 && rotation < 315)
					rotation = 270;
				else
					rotation = 0;

				if(rotation == craft.rotation) {

				}
		 */

		float rotation = (float) Math.PI * player.getLocation().getYaw() / 180f;

		// Not really sure what the N stands for...
		float nx = -(float) Math.sin(rotation);
		float nz = (float) Math.cos(rotation);

		int dx = (Math.abs(nx) >= 0.5 ? 1 : 0) * (int) Math.signum(nx);
		int dz = (Math.abs(nz) > 0.5 ? 1 : 0) * (int) Math.signum(nz);
		int dy = 0;

		if(dx != 0)
			rotation = dx * 90;
		else
			rotation = dz * 180;		

		rotation = player.getLocation().getYaw();
		if(rotation < 0)
			rotation = 360 + rotation;
		if(rotation > 45 && rotation < 135)
			rotation = 90;
		else if(rotation > 135 && rotation < 225)
			rotation = 180;
		else if (rotation > 225 && rotation < 315)
			rotation = 270;
		else
			rotation = 0;

		//player.sendMessage("Craft rotation is " + craft.rotation + ". Player rotation is " + rotation + 
		//"Difference is " + (rotation - craft.rotation));

		// we are on a flying object, handle height change
		if (craft.type.canFly || craft.type.canDive || craft.type.canDig) {

			float p = player.getLocation().getPitch();

			dy = -(Math.abs(p) >= 25 ? 1 : 0)
			* (int) Math.signum(p);

			// move straight up or straight down
			if (Math.abs(player.getLocation().getPitch()) >= 75) {
				dx = 0;
				dz = 0;
			}
		}

		int dr = (int)rotation - craft.rotation;
		if(dr < 0)
			dr = 360 + dr;
		if(dr > 360)
			dr = dr - 360;

		/*
				if(dr != 0 && dy == 0) {
					CraftRotator cr = new CraftRotator(craft);
					cr.turn(dr);
				} else */  
                                        
					CraftMover cm = new CraftMover(craft);
					cm.calculatedMove(dx, dy, dz);
				
	}

	@EventHandler
	public void onPlayerAnimation(PlayerAnimationEvent event) {
		if(event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
			Player player = event.getPlayer();
			Craft craft = Craft.getCraft(player);
			if(craft != null && craft.type.listenAnimation == true) {
				playerUsedAnItem(player, craft);			
			}
		}
	}

	@EventHandler
	public void onPlayerCommandPreprocess (PlayerCommandPreprocessEvent event) {
		//public void onPlayerCommand(PlayerChatEvent event) {
		Player player = event.getPlayer();
		String[] split = event.getMessage().split(" ");
		split[0] = split[0].substring(1);
                
		//debug commands
		if(Central.getDebugManager().isDebugMode()){
			if (split[0].equalsIgnoreCase("isDataBlock")) {
				player.sendMessage(Boolean.toString(BlocksInfo.isDataBlock(Integer.parseInt(split[1]))));
			} else if (split[0].equalsIgnoreCase("isComplexBlock")) {
				player.sendMessage(Boolean.toString(BlocksInfo.isComplexBlock(Integer.parseInt(split[1]))));
				/*
			} else if (split[0].equalsIgnoreCase("findcenter")) {
				Craft craft = Craft.getCraft(player);
				Location blockLoc = new Location(player.getWorld(), craft.posX + craft.offX, craft.posY, craft.posZ + craft.offZ);
				Block mcBlock = player.getWorld().getBlockAt(blockLoc);
				mcBlock.setType(Material.GOLD_BLOCK);
				*/
			} else if (split[0].equalsIgnoreCase("finddatablocks")) {
				Craft craft = Craft.getCraft(player);
				for(DataBlock dataBlock : craft.dataBlocks) {
					Block theBlock = player.getWorld().getBlockAt(new Location(
							player.getWorld(), craft.minX + dataBlock.x, craft.minY + dataBlock.y, craft.minZ + dataBlock.z));
					theBlock.setType(Material.GOLD_BLOCK);
				}			
			} else if (split[0].equalsIgnoreCase("findcomplexblocks")) {
				Craft craft = Craft.getCraft(player);
				for(DataBlock dataBlock : craft.complexBlocks) {
					Block theBlock = player.getWorld().getBlockAt(new Location(
							player.getWorld(), craft.minX + dataBlock.x, craft.minY + dataBlock.y, craft.minZ + dataBlock.z));
					theBlock.setType(Material.GOLD_BLOCK);
				}
			} else if (split[0].equalsIgnoreCase("diamondit")) {
				Craft craft = Craft.getCraft(player);
				
				for (int x = 0; x < craft.sizeX; x++) {
					for (int y = 0; y < craft.sizeY; y++) {
						for (int z = 0; z < craft.sizeZ; z++) {
							if(craft.matrix[x][y][z] != -1) {
								Block theBlock = player.getWorld().getBlockAt(new Location(
									player.getWorld(), craft.minX + x, craft.minY + y, craft.minZ + z));
								theBlock.setType(Material.DIAMOND_BLOCK);
							}
						}
					}
				}
			} else if (split[0].equalsIgnoreCase("craftvars")) {
				Craft craft = Craft.getCraft(player);
				
				Central.debugMessage("Craft type: " + craft.type, 4);
				Central.debugMessage("Craft name: " + craft.name, 4);
				
				//may need to make multidimensional
				Central.debugMessage("Craft matrix size: " + craft.matrix.length, 4);
				Central.debugMessage("Craft block count: " + craft.blockCount, 4);
				Central.debugMessage("Craft data block count: " + craft.dataBlocks.size(), 4);
				Central.debugMessage("Craft complex block count: " + craft.complexBlocks.size(), 4);

				Central.debugMessage("Craft speed: " + craft.speed, 4);
				Central.debugMessage("Craft size: " + craft.sizeX + " * " + craft.sizeY + " * " + craft.sizeZ, 4);
				//MoveCraft.instance.DebugMessage("Craft position: " + craft.posX + ", " + craft.posY + ", " + craft.posZ, 4);
				Central.debugMessage("Craft last move: " + craft.lastMove, 4);
				//world?
				Central.debugMessage("Craft center: " + craft.centerX + ", " + craft.centerZ, 4);
				
				Central.debugMessage("Craft water level: " + craft.waterLevel, 4);
				Central.debugMessage("Craft new water level: " + craft.newWaterLevel, 4);
				Central.debugMessage("Craft water type: " + craft.waterType, 4);
				
				Central.debugMessage("Craft bounds: " + craft.minX + "->" + craft.maxX + ", "
						+ craft.minY + "->" + craft.maxY + ", "
						+ craft.minZ + "->" + craft.maxZ, 4);
				
			} else if (split[0].equalsIgnoreCase("getRotation")) {
				Block examineBlock = player.getTargetBlock(null, 100);
				
				int blockDirection = BlocksInfo.getCardinalDirectionFromData(examineBlock.getTypeId(), examineBlock.getData());
				player.sendMessage("Block data is " + examineBlock.getData() + " direction is " + blockDirection);
			}
		}

		if (split[0].equalsIgnoreCase("movecraft")) {
			if (!PermissionInterface.CheckPermission(player, "movecraft." + event.getMessage().substring(1))) {
				return;
			}

			if (split.length >= 2) {
				if (split[1].equalsIgnoreCase("types")) {

					for (CraftType craftType : CraftType.craftTypes) {						
						if(craftType.canUse(player))
							player.sendMessage(ChatColor.GREEN + craftType.name + ChatColor.YELLOW
									+ craftType.minBlocks + "-"
									+ craftType.maxBlocks + " blocks" + " speed : "
									+ craftType.maxSpeed);
					}					
				} else if (split[1].equalsIgnoreCase("list")) {
					// list all craft currently controlled by a player

					if (Craft.craftList.isEmpty()) {
						player.sendMessage(ChatColor.YELLOW + "no player controlled craft");
						// return true;
					}

					for (Craft craft : Craft.craftList) {

						player.sendMessage(ChatColor.YELLOW + craft.name
								+ " controlled by " + craft.player.getName()
								+ " : " + craft.blockCount + " blocks");
					}
				} else if (split[1].equalsIgnoreCase("reload")) {
					Central.getConfigManager().loadProperties();
					player.sendMessage(ChatColor.YELLOW + "MoveCraft configuration reloaded");
					event.setCancelled(true);
					return;
				} else if (split[1].equalsIgnoreCase("debug")) {
					Central.getDebugManager().toggleDebug();
					event.setCancelled(true);
					return;
				} else if (split[1].equalsIgnoreCase("loglevel")) {
					try
					{
						Integer.parseInt(split[2]);
						Central.getConfigManager().getConfigFile().ConfigSettings.put("LogLevel", split[2]);
					}
					catch (Exception ex) {
						player.sendMessage("Invalid loglevel.");
					}
					event.setCancelled(true);
					return;	
				}
				else if (split[1].equalsIgnoreCase("config")) {
					Central.getConfigManager().getConfigFile().ListSettings(player);
					return;
				}
			}
			else {
				player.sendMessage(ChatColor.WHITE + "MoveCraft " + Central.getVersion() + " commands :");
				player.sendMessage(ChatColor.YELLOW + "/movecraft types "
						+ " : " + ChatColor.WHITE + "list the types of craft available");
				player.sendMessage(ChatColor.YELLOW + "/movecraft list : " + ChatColor.WHITE + "list the current player controled craft");
				player.sendMessage(ChatColor.YELLOW + "/movecraft reload : " + ChatColor.WHITE + "reload config files");
				player.sendMessage(ChatColor.YELLOW + "/[craft type] "
						+ " : " + ChatColor.WHITE + "commands specific to the craft type");
			}
			event.setCancelled(true);
		} else if (split[0].equalsIgnoreCase("release")) {
			Central.getPluginInstance().releaseCraft(player, Craft.getCraft(player));
			event.setCancelled(true);
		} else if (split[0].equalsIgnoreCase("remote")) {
			player.sendMessage("0");
			Craft craft = Craft.getCraft(player);
			if(craft != null) {
				split[0] = craft.type.name;
				split[1] = "remote";

				if (!PermissionInterface.CheckPermission(player, "movecraft." + event.getMessage().substring(1))) {
					event.setCancelled(true);
					return;
				}
				player.sendMessage("1");
				if(processCommand(craft.type, player, split) == true)
					event.setCancelled(true);
			} else
				player.sendMessage("You have no craft to remote :( Hurry and get one before they're sold out!");
		} else {
			String craftName = split[0];

			CraftType craftType = CraftType.getCraftType(craftName);

			if (craftType != null) {
				if (!PermissionInterface.CheckPermission(player, "movecraft." + event.getMessage().substring(1))) {
					event.setCancelled(true);
					return;
				}
				if(processCommand(craftType, player, split) == true)
					event.setCancelled(true);
			} else {
				Craft craft = Craft.getCraft(player);
				
				if(craft == null)
					return;
				
				int i = 0;
				while(i < split.length) {
					String tmpName = split[0];
					//build out tmpName with 0 + i 
					if(tmpName.equalsIgnoreCase(craft.name)) {
						if (!PermissionInterface.CheckPermission(player, "movecraft." + event.getMessage().substring(1))) {
							event.setCancelled(true);
							return;
						}
						if(processCommand(craftType, player, split) == true)
							event.setCancelled(true);						
					}
					i++;
				}
			}
		}

		return;
	}

	public boolean processCommand(CraftType craftType, Player player, String[] split) {

		Craft craft = Craft.getCraft(player);

		if (split.length >= 2) {

			if(craft == null &&
					!split[1].equalsIgnoreCase(craftType.driveCommand) &&
					!split[1].equalsIgnoreCase("remote"))
				return false;

			if (split[1].equalsIgnoreCase(craftType.driveCommand)) {

				/*
				if(!craftType.canUse(player)){
					player.sendMessage(ChatColor.RED + "You are not allowed to use this type of craft");
					return false;
				}
				*/
				
				String name = craftType.name; 
				if(split.length > 2 && split[2] != null)
					name = split[2];

				// try to detect and create the craft
				// use the block the player is standing on
				Central.getPluginInstance().createCraft(player, craftType,
						(int) Math.floor(player.getLocation().getX()),
						(int) Math.floor(player.getLocation().getY() - 1),
						(int) Math.floor(player.getLocation().getZ()), name);

				return true;

			} else if (split[1].equalsIgnoreCase("move")) {
				try {
					int dx = Integer.parseInt(split[2]);
					int dy = Integer.parseInt(split[3]);
					int dz = Integer.parseInt(split[4]);
					
					CraftMover cm = new CraftMover(craft);
					cm.calculatedMove(dx, dy, dz);
				} catch (Exception ex) {
					player.sendMessage(ChatColor.WHITE + "Invalid movement parameters. Please use " + ChatColor.AQUA + 
							"Move x y z " + ChatColor.WHITE + " Where x, y, and z are whole numbers separated by spaces.");
				}
				return true;
			} else if (split[1].equalsIgnoreCase("setspeed")) {
				int speed = Math.abs(Integer.parseInt(split[2]));

				if (speed < 1 || speed > craftType.maxSpeed) {
					player.sendMessage(ChatColor.YELLOW + "Allowed speed between 1 and "
							+ craftType.maxSpeed);
					return true;
				}

				craft.setSpeed(speed);
				player.sendMessage(ChatColor.YELLOW + craft.name + "'s speed set to "
						+ craft.speed);

				return true;

			} else if (split[1].equalsIgnoreCase("setname")) {
				craft.name = split[2];
				player.sendMessage(ChatColor.YELLOW + craft.type.name + "'s name set to "
						+ craft.name);
				return true;
				
			} else if (split[1].equalsIgnoreCase("remote")) {
				if(craft == null || craft.type != craftType) {
					Block targetBlock = player.getTargetBlock(null, 100);
					
					if(targetBlock != null) {						
						Central.getPluginInstance().createCraft(player, craftType,
								targetBlock.getX(),
								targetBlock.getY(),
								targetBlock.getZ(), 
								null);
						Craft.getCraft(player).setIsOnBoard(false);
					} else {
						player.sendMessage("Couldn't find a target within 100 blocks. " + 
								"If your admin asks reeeaaaaaally nicely, I might add distance as a config setting.");
					}					
					
					return true;
				}
				
				if (craft.isOnCraft(player, true)) {
					player.sendMessage(ChatColor.YELLOW + "You are on the " + craftType.name
							+ ", remote control not possible");
				} else {
					if (craft.haveControl) {
						player.sendMessage(ChatColor.YELLOW + "You switch off the remote controller");
					} else {
						MoveCraft_Timer timer = MoveCraft_Timer.playerTimers.get(player);
						if(timer != null)
							timer.Destroy();
						player.sendMessage(ChatColor.YELLOW + "You switch on the remote controller");
					}

					craft.haveControl = !craft.haveControl;
				}

				return true;

			} else if (split[1].equalsIgnoreCase("release")) {
				Central.getPluginInstance().releaseCraft(player, craft);
				return true;

			} else if (split[1].equalsIgnoreCase("info")) {

				player.sendMessage(ChatColor.WHITE + craftType.name);
				if(craft != null)
					player.sendMessage(ChatColor.YELLOW + "Using " + craft.blockCount + " of " + 
							craftType.maxBlocks + " blocks (minimum " + craftType.minBlocks + ").");
							//Integer.toString(craftType.minBlocks) + "-" + craftType.maxBlocks + " blocks." + 
							//" (Using " +  + ".)");
				else
					player.sendMessage(ChatColor.YELLOW + 
							Integer.toString(craftType.minBlocks) + "-" + craftType.maxBlocks + " blocks.");
				player.sendMessage(ChatColor.YELLOW +"Max speed: " + craftType.maxSpeed);

				if (Central.getDebugManager().isDebugMode()) {
					player.sendMessage(ChatColor.YELLOW + Integer.toString(craft.dataBlocks.size()) + " data Blocks, " + 
							craft.complexBlocks.size() + " complex Blocks, " + 
							craft.engineBlocks.size() + " engine Blocks," + 
							craft.digBlockCount + " drill bits.");
				}
				
				//player.sendMessage("Engine block ID: " + craft.type.engineBlockId);

				String canDo = ChatColor.YELLOW + craftType.name + "s can ";

				if (craftType.canFly)
					canDo += "fly, ";

				if (craftType.canDive)
					canDo += "dive, ";

				if(craftType.canDig)
					canDo += "dig, ";

				if (craftType.canNavigate)
					canDo += " navigate on both water and lava, ";

				player.sendMessage(canDo);

				if (craftType.flyBlockType != 0) {
					int flyBlocksNeeded = (int)Math.floor((craft.blockCount - craft.flyBlockCount) * ((float)craft.type.flyBlockPercent * 0.01) / (1 - ((float)craft.type.flyBlockPercent * 0.01)));

					if(flyBlocksNeeded < 1)
						flyBlocksNeeded = 1;

					player.sendMessage(ChatColor.YELLOW + "Flight requirement: "
							+ craftType.flyBlockPercent + "%" + " of "
							+ BlocksInfo.getName(craft.type.flyBlockType)
							+ "(" + flyBlocksNeeded + ")");
				}

				if(craft.type.fuelItemId != 0) {
					player.sendMessage(craft.remainingFuel + " units of fuel on board. " + 
							"Movement requires type " + craft.type.fuelItemId);
				}

				return true;

			} else if (split[1].equalsIgnoreCase("hyperspace")) {				
				if(!craft.inHyperSpace)
					Craft_Hyperspace.enterHyperSpace(craft);
				else
					Craft_Hyperspace.exitHyperSpace(craft);
				return true;
			} else if (split[1].equalsIgnoreCase("addwaypoint")) {
				//if(split[2].equalsIgnoreCase("absolute"))
				if(split[2].equalsIgnoreCase("relative")) {
					Location newLoc = craft.WayPoints.get(craft.WayPoints.size() - 1);
					if(!split[3].equalsIgnoreCase("0"))
						newLoc.setX(newLoc.getX() + Integer.parseInt(split[3]));
					else if(!split[4].equalsIgnoreCase("0"))
						newLoc.setY(newLoc.getY() + Integer.parseInt(split[4]));
					else if(!split[5].equalsIgnoreCase("0"))
						newLoc.setZ(newLoc.getZ() + Integer.parseInt(split[5]));

					craft.addWayPoint(newLoc);
				} else
					craft.addWayPoint(player.getLocation());

				player.sendMessage("Added waypoint...");

			} else if (split[1].equalsIgnoreCase("autotravel")) {
				if(split[2].equalsIgnoreCase("true"))
					new MoveCraft_Timer(0, craft, "automove", true);
				else
					new MoveCraft_Timer(0, craft, "automove", false);

			} else if (split[1].equalsIgnoreCase("turn")) {
				//if(!player.getName().equalsIgnoreCase("sycoprime"))
					//return false;
				
				if(split[2].equalsIgnoreCase("right"))
					craft.turn(90);
				else if (split[2].equalsIgnoreCase("left"))
					craft.turn(270);
				else if (split[2].equalsIgnoreCase("around"))
					craft.turn(180);
				return true;
			} else if (split[1].equalsIgnoreCase("warpdrive")) {
				//if the player just said "warpdrive", list the worlds they can warp to
				if(split.length == 1) {
					List<World> worlds = Central.getPluginInstance().getServer().getWorlds();
					player.sendMessage("You can warp to: ");
					for(World world : worlds) {
						player.sendMessage(world.getName());
					}
				} else {
					World targetWorld = Central.getPluginServer().getWorld(split[2]);
					if(targetWorld != null) {
						craft.WarpToWorld(targetWorld);
					} else if(player.isOp()) { //create the world, if the player is an op
						if(split.length > 3 && split[3].equalsIgnoreCase("nether")) {
                                                    WorldCreator wc = new WorldCreator(split[2]);
                                                    wc.environment(Environment.NETHER);
                                                    Central.getPluginServer().createWorld(wc);
						}
						else {
                                                    WorldCreator wc = new WorldCreator(split[2]);
                                                    wc.environment(Environment.NORMAL);
                                                    Central.getPluginServer().createWorld(wc);
						}
						
						while(targetWorld == null) {
							try {
								wait(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							targetWorld = Central.getPluginServer().getWorld(split[2]);
						}
						Chunk targetChunk = targetWorld.getChunkAt(new Location(targetWorld, craft.minX, craft.minY, craft.minZ));
						targetWorld.loadChunk(targetChunk);
						
						craft.WarpToWorld(targetWorld);
					}
				}
			}
		}

		player.sendMessage(ChatColor.WHITE + "MoveCraft " + Central.getVersion() + " commands :");
		player.sendMessage(ChatColor.YELLOW + "/" + craftType.name + " "
				+ craftType.driveCommand + " : " + ChatColor.WHITE + "" + " "
				+ craftType.driveCommand + " the " + craftType.name);
		player.sendMessage(ChatColor.YELLOW + "/" + craftType.name + " "
				+ "release : " + ChatColor.WHITE + "release the " + craftType.name);
		player.sendMessage(ChatColor.YELLOW + "/" + craftType.name + " "
				+ "remote : " + ChatColor.WHITE + "remote control of the " + craftType.name);
		player.sendMessage(ChatColor.YELLOW + "/" + craftType.name + " "
				+ "size : " + ChatColor.WHITE + "the size of the " + craftType.name + " in block");
		player.sendMessage(ChatColor.YELLOW + "/" + craftType.name + " "
				+ "setname : " + ChatColor.WHITE + "set the " + craftType.name + "'s name");
		player.sendMessage(ChatColor.YELLOW + "/" + craftType.name + " "
				+ "info : " + ChatColor.WHITE + "displays informations about the " + craftType.name);

		return true;
	}
}
