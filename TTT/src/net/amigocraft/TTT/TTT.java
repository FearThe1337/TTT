package net.amigocraft.TTT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.amigocraft.TTT.AutoUpdate;
import net.amigocraft.TTT.Metrics;
import net.amigocraft.TTT.utils.WorldUtils;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class TTT extends JavaPlugin implements Listener {

	// Special thanks to:
	// ------------------
	// ZerosAce00000
	// momhipie
	// JHA
	// Jack M.
	// ------------------
	// for being this plugin's first alpha testers

	public static Logger log = Logger.getLogger("Minecraft");
	public static TTT plugin = new TTT();

	public HashMap<String, String> joinedPlayers = new HashMap<String, String>();
	public HashMap<String, Integer> playerRoles = new HashMap<String, Integer>();
	public HashMap<String, Integer> time = new HashMap<String, Integer>();
	public HashMap<String, Integer> tasks = new HashMap<String, Integer>();
	public HashMap<String, Integer> gameTime = new HashMap<String, Integer>();
	public HashMap<String, String> deadPlayers = new HashMap<String, String>();
	public List<Body> bodies = new ArrayList<Body>();
	public List<Body> foundBodies = new ArrayList<Body>();

	@Override
	public void onEnable(){
		// autoupdate
		if (getConfig().getBoolean("enable-auto-update")){
			try {new AutoUpdate(this);}
			catch (Exception e){e.printStackTrace();}
		}

		// submit metrics
		if (getConfig().getBoolean("enable-metrics")){
			try {
				Metrics metrics = new Metrics(this);
				metrics.start();
			}
			catch (IOException e) {log.warning("[GoldBank] Failed to submit statistics to Plugin Metrics");}
		}

		// register events and the plugin variable
		getServer().getPluginManager().registerEvents(this, this);
		TTT.plugin = this;

		// create the default config
		if(!(new File(plugin.getDataFolder(), "config.yml")).exists())
			plugin.saveDefaultConfig();

		log.info(this + " has been enabled!");
	}

	@Override
	public void onDisable(){
		log.info(this + " has been disabled!");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		if (commandLabel.equalsIgnoreCase("ttt")){
			if (args.length > 0){
				if (args[0].equalsIgnoreCase("import")){
					if (sender.hasPermission("ttt.import")){
						if (args.length > 1){
							File folder = new File(args[1]);
							if (folder.exists()){
								if (!args[1].substring(0, 3).equalsIgnoreCase("TTT_")){
									if (WorldUtils.isWorld(folder)){
										File newFolder = new File("TTT_" + args[1]);
										if (!newFolder.exists()){
											try {
												FileUtils.copyDirectory(folder, newFolder);
												sender.sendMessage(ChatColor.GREEN + "Successfully imported world!");
											}
											catch (IOException e){
												sender.sendMessage(ChatColor.RED + "An error occurred while creating the new folder");
												e.printStackTrace();
											}
										}
										else
											sender.sendMessage(ChatColor.RED + "Error: This world has already been imported!");
										newFolder = null;
									}
									else
										sender.sendMessage(ChatColor.RED + "Error: The specified world cannot be loaded, and therefore is invalid or corrupt");
								}
								else
									sender.sendMessage(ChatColor.RED + "Error: Folder name must not start with \"TTT_\"!");
							}
							else
								sender.sendMessage(ChatColor.RED + "Error: Specified folder cannot be found! Verify that the folder is in the server's root directory, then try again.");
							folder = null;
						}
						else
							sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /ttt import [folder name]");
					}
					else
						sender.sendMessage(ChatColor.RED + "You do not have permission to import a map!");
				}
				else if (args[0].equalsIgnoreCase("join")){
					if (sender instanceof Player){
						if (sender.hasPermission("ttt.join")){
							if (args.length > 1){
								if (gameTime.get(args[1]) == null){
									File folder = new File(args[1]);
									File tttFolder = new File("TTT_" + args[1]);
									if (folder.exists() && tttFolder.exists()){
										boolean loaded = false;
										for (World w : Bukkit.getServer().getWorlds()){
											if(w.getName().equals("TTT_" + args[1])){
												loaded = true;
												break;
											}
										}
										final String worldName = args[1];
										if (!loaded){
											getServer().createWorld(new WorldCreator("TTT_" + worldName));
										}
										((Player)sender).teleport(getServer().getWorld("TTT_" + worldName).getSpawnLocation());
										joinedPlayers.put(((Player)sender).getName(), worldName);
										sender.sendMessage(ChatColor.GREEN + "Successfully joined map " + worldName);
										Bukkit.broadcastMessage(ChatColor.DARK_PURPLE + "[TTT] " + sender.getName() + " has joined map \"" + worldName + "\"");
										if (joinedPlayers.size() >= getConfig().getInt("minimum-players") && !time.containsKey(worldName)){
											for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers())
												p.sendMessage(ChatColor.DARK_PURPLE + "Round is starting!");
											time.put(worldName, getConfig().getInt("setup-time"));
											tasks.put(worldName, setupTimer(worldName));
										}
										else {
											((Player)sender).sendMessage(ChatColor.DARK_PURPLE + "Waiting for players...");
										}
									}
									else
										sender.sendMessage(ChatColor.RED + "Error: The specified map does not exist or has not been imported!!");
									folder = null;
									tttFolder = null;
								}
								else
									sender.sendMessage(ChatColor.RED + "You may not join a game already in progress!");
							}
							else
								sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /tt join [world name]");
						}
						else
							sender.sendMessage(ChatColor.RED + "You do not have permission to join a game!");
					}
					else
						sender.sendMessage(ChatColor.RED + "You must be an ingame player to perform this command!");
				}
				else if (args[0].equalsIgnoreCase("quit")){
					if (sender instanceof Player){
						if (sender.hasPermission("ttt.quit")){
							if (joinedPlayers.containsKey(sender.getName()) || deadPlayers.containsKey(sender.getName())){
								((Player)sender).teleport(getServer().getWorlds().get(0).getSpawnLocation());
								joinedPlayers.remove(sender.getName());
								deadPlayers.remove(sender.getName());
								playerRoles.remove(sender.getName());
							}
							else
								sender.sendMessage(ChatColor.RED + "You are not currently in a game!");
						}
						else
							sender.sendMessage(ChatColor.RED + "You do not have permission to leave a game!");
					}
					else
						sender.sendMessage(ChatColor.RED + "You must be an ingame player to perform this command!");
				}
				else
					sender.sendMessage(ChatColor.RED + "Invalid command! Usage: /ttt [command]");
			}
			else
				sender.sendMessage(ChatColor.RED + "Too few arguments! Usage: /ttt [command]");
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent e){
		if (e.getEntityType() == EntityType.PLAYER){
			Player p = (Player)e.getEntity();
			int armor = 0;
			if (e.getCause() == DamageCause.ENTITY_ATTACK ||
					e.getCause() == DamageCause.PROJECTILE ||
					e.getCause() == DamageCause.FIRE ||
					e.getCause() == DamageCause.FIRE_TICK ||
					e.getCause() == DamageCause.BLOCK_EXPLOSION || 
					e.getCause() == DamageCause.CONTACT ||
					e.getCause() == DamageCause.LAVA ||
					e.getCause() == DamageCause.ENTITY_EXPLOSION){
				HashMap<Material, Integer> protection = new HashMap<Material, Integer>();
				protection.put(Material.LEATHER_HELMET, 1);
				protection.put(Material.LEATHER_CHESTPLATE, 3);
				protection.put(Material.LEATHER_LEGGINGS, 2);
				protection.put(Material.LEATHER_BOOTS, 1);
				protection.put(Material.IRON_HELMET, 2);
				protection.put(Material.IRON_CHESTPLATE, 5);
				protection.put(Material.IRON_LEGGINGS, 3);
				protection.put(Material.IRON_BOOTS, 1);
				protection.put(Material.CHAINMAIL_HELMET, 2);
				protection.put(Material.CHAINMAIL_CHESTPLATE, 5);
				protection.put(Material.CHAINMAIL_LEGGINGS, 3);
				protection.put(Material.CHAINMAIL_BOOTS, 1);
				protection.put(Material.GOLD_HELMET, 2);
				protection.put(Material.GOLD_CHESTPLATE, 6);
				protection.put(Material.GOLD_LEGGINGS, 5);
				protection.put(Material.GOLD_BOOTS, 2);
				protection.put(Material.DIAMOND_HELMET, 3);
				protection.put(Material.DIAMOND_CHESTPLATE, 8);
				protection.put(Material.DIAMOND_LEGGINGS, 6);
				protection.put(Material.DIAMOND_BOOTS, 3);
				if (p.getInventory().getArmorContents()[0] != null)
					if (protection.containsKey(p.getInventory().getArmorContents()[0].getType()))
						armor += protection.get(p.getInventory().getArmorContents()[0].getType());
				if (p.getInventory().getArmorContents()[1] != null)
					if (protection.containsKey(p.getInventory().getArmorContents()[1].getType()))
						armor += protection.get(p.getInventory().getArmorContents()[1].getType());
				if (p.getInventory().getArmorContents()[2] != null)
					if (protection.containsKey(p.getInventory().getArmorContents()[2].getType()))
						armor += protection.get(p.getInventory().getArmorContents()[2].getType());
				if (p.getInventory().getArmorContents()[3] != null)
					if (protection.containsKey(p.getInventory().getArmorContents()[3].getType()))
						armor += protection.get(p.getInventory().getArmorContents()[3].getType());
			}
			if (e.getDamage() - ((armor * .04) * e.getDamage()) >= ((Player)e.getEntity()).getHealth()){
				if (joinedPlayers.containsKey(((Player)e.getEntity()).getName())){
					e.setCancelled(true);
					p.setHealth(20);
					p.sendMessage(ChatColor.DARK_PURPLE + "You are now dead! You have been hidden from other players and are now capable of flying.");
					String worldName = p.getWorld().getName().replace("TTT_", "");
					joinedPlayers.remove(p.getName());
					deadPlayers.put(p.getName(), worldName);
					Block block = p.getLocation().getBlock();
					block.setType(Material.CHEST);
					Chest chest = (Chest)block.getState();
					// player identifier
					ItemStack id = new ItemStack(Material.PAPER, 1);
					ItemMeta idMeta = id.getItemMeta();
					idMeta.setDisplayName("ID");
					List<String> idLore = new ArrayList<String>();
					idLore.add("This is the body of");
					idLore.add(((Player)e.getEntity()).getName());
					idMeta.setLore(idLore);
					id.setItemMeta(idMeta);
					// role identifier
					ItemStack ti = new ItemStack(Material.WOOL, 1);
					ItemMeta tiMeta = ti.getItemMeta();
					if (playerRoles.get(p.getName()) == 0){
						ti.setDurability((short)5);
						tiMeta.setDisplayName("�2Innocent");
						List<String> tiLore = new ArrayList<String>();
						tiLore.add("This person was innocent!");
						tiMeta.setLore(tiLore);
					}
					else {
						ti.setDurability((short)14);
						tiMeta.setDisplayName("�4Traitor");
						List<String> lore = new ArrayList<String>();
						lore.add("This person was a traitor!");
						tiMeta.setLore(lore);
					}
					ti.setItemMeta(tiMeta);
					chest.getInventory().addItem(new ItemStack[]{id, ti});
					bodies.add(new Body(p.getName(), playerRoles.get(p.getName()), FixedLocation.getFixedLocation(block)));
				}
			}
			if (deadPlayers.containsKey(p.getName())){
				e.setCancelled(true);
			}
			if (e instanceof EntityDamageByEntityEvent){
				EntityDamageByEntityEvent ed = (EntityDamageByEntityEvent)e;
				if (ed.getDamager().getType() == EntityType.PLAYER){
					if (((Player)ed.getDamager()).getItemInHand() != null)
						if (((Player)ed.getDamager()).getItemInHand().getItemMeta() != null)
							if (((Player)ed.getDamager()).getItemInHand().getItemMeta().getDisplayName() != null)
								if (((Player)ed.getDamager()).getItemInHand().getItemMeta().getDisplayName().equals("Crowbar"))
									e.setDamage(getConfig().getInt("crowbar-damage"));
					if (deadPlayers.containsKey(((Player)ed.getDamager()).getName()))
						e.setCancelled(true);
				}
			}
		}
	}

	public int setupTimer(final String worldName){
		return getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			public void run(){
				// verify that all players are still online
				List<String> offlinePlayers = new ArrayList<String>();
				for (String pl : joinedPlayers.keySet()){
					if (joinedPlayers.get(pl).equals(worldName)){
						Player p = getServer().getPlayer(pl);
						if (p != null){
							if (!getServer().getWorld("TTT_" + worldName).getPlayers().contains(p)){
								offlinePlayers.add(pl);
								log.info("pl 1");
							}
						}
					}
				}
				for (String pl : deadPlayers.keySet()){
					if (deadPlayers.get(pl).equals(worldName)){
						Player p = getServer().getPlayer(pl);
						if (p != null){
							if (!getServer().getWorld("TTT_" + worldName).getPlayers().contains(p)){
								offlinePlayers.add(pl);
								Bukkit.broadcastMessage("[TTT]" + pl + " has left map \"" + worldName + "\"");
							}
						}
					}
				}
				for (String p : offlinePlayers){
					if (joinedPlayers.containsKey(p)){
						joinedPlayers.remove(p);
					}
					if (deadPlayers.containsKey(p)){
						deadPlayers.remove(p);
					}
				}
				int currentTime = time.get(worldName);
				int playerCount = 0; 
				for (String p : joinedPlayers.keySet()){
					if (joinedPlayers.get(p).equals(worldName))
						playerCount += 1;
				}
				if (playerCount >= getConfig().getInt("minimum-players")){
					if((currentTime % 10) == 0 && currentTime > 0){
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + "The game will begin in " + currentTime + " seconds!");
						}
					}
					else if (currentTime > 0 && currentTime < 10){
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + "The game will begin in " + currentTime + " seconds!");
						}
					}
					else if (currentTime <= 0){
						int players = getServer().getWorld("TTT_" + worldName).getPlayers().size();
						int traitors = 0;
						int limit = (int)(players * getConfig().getDouble("traitor-ratio"));
						if (limit == 0)
							limit = 1;
						List<String> innocents = new ArrayList<String>();
						List<String> traitorNames = new ArrayList<String>();
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							innocents.add(p.getName());
							p.sendMessage(ChatColor.DARK_PURPLE + "The game has begun!");
						}
						while (traitors < limit){
							Random randomGenerator = new Random();
							int index = randomGenerator.nextInt(players);
							String traitor = innocents.get(index);
							if (innocents.contains(traitor)){
								innocents.remove(traitor);
								traitorNames.add(traitor);
								traitors += 1;
							}
						}
						ItemStack crowbar = new ItemStack(Material.IRON_SWORD, 1);
						ItemMeta cbMeta = crowbar.getItemMeta();
						cbMeta.setDisplayName("Crowbar");
						crowbar.setItemMeta(cbMeta);
						ItemStack gun = new ItemStack(Material.ANVIL, 1);
						ItemMeta gunMeta = crowbar.getItemMeta();
						gunMeta.setDisplayName("Gun");
						gun.setItemMeta(gunMeta);
						ItemStack ammo = new ItemStack(Material.ARROW, 28);
						for (String p : joinedPlayers.keySet()){
							Player pl = getServer().getPlayer(p);
							if (innocents.contains(p)){
								playerRoles.put(p, 0);
								pl.sendMessage(ChatColor.DARK_GREEN + "You are an innocent! But beware, there are traitors lurking about, yearning to fill you with bullets.");
								pl.getInventory().addItem(new ItemStack[]{crowbar, gun, ammo});
							}
							else {
								playerRoles.put(p, 1);
								pl.sendMessage(ChatColor.DARK_RED + "You are a traitor! Your job is to kill all of the innocents. But beware, you are outnumbered. You'll need cleverness and agility if you are to win this.");
								if (traitorNames.size() > 1){
									pl.sendMessage(ChatColor.DARK_RED + "Traitor, these are your allies:");
									for (String t : traitorNames)
										pl.sendMessage("- " + t);
								}
								else
									pl.sendMessage(ChatColor.DARK_RED + "Traitor, you stand alone.");
								pl.getInventory().addItem(new ItemStack[]{crowbar, gun, ammo});
							}
						}
						time.remove(worldName);
						gameTime.put(worldName, getConfig().getInt("time-limit"));
						Bukkit.getScheduler().cancelTask(tasks.get(worldName));
						tasks.remove(worldName);
						gameTimer(worldName);
					}
					if (currentTime > 0)
						time.put(worldName, currentTime - 1);
				}
				else {
					time.remove(worldName);
					Bukkit.getScheduler().cancelTask(tasks.get(worldName));
					for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
						p.sendMessage(ChatColor.DARK_PURPLE + "Waiting for players...");
					}
				}
			}
		}, 0L, 20L);
	}

	public void gameTimer(final String worldName){
		tasks.put(worldName, getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			public void run(){
				// verify that all players are still online
				List<String> offlinePlayers = new ArrayList<String>();
				for (String pl : joinedPlayers.keySet()){
					if (joinedPlayers.get(pl).equals(worldName)){
						Player p = getServer().getPlayer(pl);
						if (p != null){
							if (!getServer().getWorld("TTT_" + worldName).getPlayers().contains(p)){
								offlinePlayers.add(pl);
								log.info(pl);
							}
						}
					}
				}
				for (String pl : deadPlayers.keySet()){
					if (deadPlayers.get(pl).equals(worldName)){
						Player p = getServer().getPlayer(pl);
						if (p != null){
							if (!getServer().getWorld("TTT_" + worldName).getPlayers().contains(p)){
								offlinePlayers.add(pl);
								Bukkit.broadcastMessage("[TTT]" + pl + " has left map \"" + worldName + "\"");
							}
						}
					}
				}
				for (String p : offlinePlayers){
					if (joinedPlayers.containsKey(p)){
						joinedPlayers.remove(p);
					}
					if (deadPlayers.containsKey(p)){
						deadPlayers.remove(p);
					}
				}

				// check if game is over
				boolean iLeft = false;
				boolean tLeft = false;
				for (String p : playerRoles.keySet()){
					if (playerRoles.get(p) == 0 && joinedPlayers.containsKey(p)){
						if (joinedPlayers.get(p).equals(worldName)){
							iLeft = true;
						}
					}
					if (playerRoles.get(p) == 1 && joinedPlayers.containsKey(p)){
						if (joinedPlayers.get(p).equals(worldName)){
							tLeft = true;
						}
					}
				}
				if (!(tLeft && iLeft)){
					List<Body> removeBodies = new ArrayList<Body>();
					List<Body> removeFoundBodies = new ArrayList<Body>(); 
					for (Body b : bodies){
						if (deadPlayers.get(b.getName()) != null){
							if (deadPlayers.get(b.getName()).equals(worldName)){
								removeBodies.add(b);
								if (foundBodies.contains(b))
									removeFoundBodies.add(b);
							}
						}
					}

					for (Body b : removeBodies)
						bodies.remove(b);

					for (Body b : removeFoundBodies)
						foundBodies.remove(b);

					removeBodies.clear();
					removeFoundBodies.clear();

					if (!tLeft)
						Bukkit.broadcastMessage(ChatColor.DARK_GREEN + "[TTT] The innocents won on map \"" + worldName + "\"!");
					if (!iLeft)
						Bukkit.broadcastMessage(ChatColor.DARK_RED + "[TTT] The traitors won on map \"" + worldName + "\"!");
					for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
						joinedPlayers.remove(p.getName());
						playerRoles.remove(p.getName());
						if (deadPlayers.containsKey(p.getName()))
							deadPlayers.remove(p.getName());
						gameTime.remove(worldName);
						ItemStack crowbar = new ItemStack(Material.IRON_SWORD, 1);
						ItemMeta cbMeta = crowbar.getItemMeta();
						cbMeta.setDisplayName("Crowbar");
						crowbar.setItemMeta(cbMeta);
						ItemStack gun = new ItemStack(Material.ANVIL, 1);
						ItemMeta gunMeta = crowbar.getItemMeta();
						gunMeta.setDisplayName("Gun");
						gun.setItemMeta(gunMeta);
						ItemStack ammo = new ItemStack(Material.ARROW, 28);
						p.getInventory().remove(crowbar);
						p.getInventory().remove(gun);
						p.getInventory().remove(ammo);
						p.teleport(getServer().getWorlds().get(0).getSpawnLocation());
					}
					gameTime.remove(worldName);
					getServer().getScheduler().cancelTask(tasks.get(worldName));
					tasks.remove(tasks.get(worldName));
					getServer().unloadWorld("TTT_" + worldName, false);
					rollbackWorld(worldName);
				}
				else {
					int newTime = gameTime.get(worldName) - 1;
					gameTime.remove(worldName);
					gameTime.put(worldName, newTime);
					if (newTime % 60 == 0 && newTime >= 60){
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + Integer.toString(newTime / 60) + " minutes left in the game!");
						}
					}
					else if (newTime % 10 == 0 && newTime > 10 && newTime < 60){
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + Integer.toString(newTime) + " seconds left in the game!");
						}
					}
					else if (newTime < 10 && newTime > 0){
						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_PURPLE + Integer.toString(newTime) + " seconds left in the game!");
						}
					}
					else if (newTime <= 0){
						List<Body> removeBodies = new ArrayList<Body>();
						List<Body> removeFoundBodies = new ArrayList<Body>(); 
						for (Body b : bodies){
							if (deadPlayers.get(b.getName()) != null){
								if (deadPlayers.get(b.getName()).equals(worldName)){
									removeBodies.add(b);
									if (foundBodies.contains(b))
										removeFoundBodies.add(b);
								}
							}
						}

						for (Body b : removeBodies)
							bodies.remove(b);

						for (Body b : removeFoundBodies)
							foundBodies.remove(b);

						removeBodies.clear();
						removeFoundBodies.clear();

						for (Player p : getServer().getWorld("TTT_" + worldName).getPlayers()){
							p.sendMessage(ChatColor.DARK_GREEN + "The innocents won on map \"" + worldName + "\"!");
							joinedPlayers.remove(p.getName());
							playerRoles.remove(p.getName());
							if (deadPlayers.containsKey(p.getName())){
								p.setAllowFlight(false);
								for (Player pl : getServer().getOnlinePlayers())
									pl.showPlayer(p);
								deadPlayers.remove(p.getName());
							}
							gameTime.remove(worldName);
							p.teleport(getServer().getWorlds().get(0).getSpawnLocation());
						}
						Bukkit.getScheduler().cancelTask(tasks.get(worldName));
						getServer().unloadWorld("TTT_" + worldName, false);
						rollbackWorld(worldName);
					}
				}
				// hide dead players
				for (String p : deadPlayers.keySet()){
					if (getServer().getPlayer(p) != null){
						if (getServer().getWorld("TTT_" + worldName).getPlayers().contains(getServer().getPlayer(p))){
							getServer().getPlayer(p).setAllowFlight(true);
							for (String other : joinedPlayers.keySet()){
								if (joinedPlayers.get(other).equals(worldName))
									getServer().getPlayer(other).hidePlayer(getServer().getPlayer(p));
							}
						}
					}
				}
			}
		}, 0L, 20L));
	}

	public void rollbackWorld(final String worldName){
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
			public void run(){
				File folder = new File(worldName);
				if (folder.exists()){
					if (WorldUtils.isWorld(folder)){
						File newFolder = new File("TTT_" + worldName);
						try {
							FileUtils.copyDirectory(folder, newFolder);
							log.info("Successfully rolled back world!");
						}
						catch (IOException ex){
							log.info("An error occurred while recreating the new world folder for " + worldName);
							ex.printStackTrace();
						}
					}
					else
						log.info("Error: The world cannot be loaded, and therefore is invalid or corrupt");
				}
			}
		}, 100L);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClick(InventoryClickEvent e){
		log.info("it fired");
		for (HumanEntity he : e.getViewers()){
			Player p = (Player)he;
			if (joinedPlayers.containsKey(p.getName())){
				log.info("they're joined");
				if (e.getInventory().getType() == InventoryType.CHEST){
					log.info("it's a chest");
					Block block = ((Chest)e.getInventory().getHolder()).getBlock();
					for (Body b : bodies){
						if (b.getLocation().equals(FixedLocation.getFixedLocation(block))){
							e.setCancelled(true);
							log.info("it's a body");
							break;
						}
					}
				}
			}
			else if (deadPlayers.containsKey(p.getName()))
				e.setCancelled(true);
		}
	}

	@EventHandler (priority = EventPriority.HIGH)
	public void onPlayerChat(AsyncPlayerChatEvent e){
		for (Player p : getServer().getOnlinePlayers()){
			// check if sender is in TTT game
			if (joinedPlayers.containsKey(e.getPlayer().getName())){
				if (joinedPlayers.containsKey(p.getName()) || deadPlayers.containsKey(p.getName())){
					if (!p.getWorld().getName().equals(e.getPlayer().getWorld().getName()))
						e.getRecipients().remove(p);
				}
				else
					e.getRecipients().remove(p);
			}

			// check if sender is dead
			else if (deadPlayers.containsKey(e.getPlayer().getName())){
				if (deadPlayers.containsKey(p.getName())){
					if (!p.getWorld().getName().equals("TTT_" + deadPlayers.get(e.getPlayer().getName())))
						e.getRecipients().remove(p);
				}
				else
					e.getRecipients().remove(p);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent e){
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK){
			if (e.getClickedBlock().getType() == Material.CHEST){
				int index = -1;
				for (int i = 0; i < bodies.size(); i++){
					if (bodies.get(i).getLocation().equals(FixedLocation.getFixedLocation(e.getClickedBlock()))){
						index = i;
						break;
					}
				}
				if (index != -1){
					boolean found = false;
					for (Body b : foundBodies){
						if (b.getLocation().equals(FixedLocation.getFixedLocation(e.getClickedBlock())))
							found = true;
					}
					if (!found){
						for (Player p : e.getPlayer().getWorld().getPlayers()){
							if (bodies.get(index).getRole() == 0)
								p.sendMessage(ChatColor.DARK_GREEN + e.getPlayer().getName() + " found the body of " + bodies.get(index).getName() + ". He was innocent.");
							else if (bodies.get(index).getRole() == 1)
								p.sendMessage(ChatColor.DARK_RED + e.getPlayer().getName() + " found the body of " + bodies.get(index).getName() + ". He was a traitor!.");
						}
						foundBodies.add(bodies.get(index));
					}
				}
			}
		}
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR){
			if (e.getPlayer().getItemInHand() != null){
				if (e.getPlayer().getItemInHand().getItemMeta() != null){
					if (e.getPlayer().getItemInHand().getItemMeta().getDisplayName() != null){
						if (e.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals("Gun")){
							if ((joinedPlayers.containsKey(e.getPlayer().getName()) || getConfig().getBoolean("guns-outside-arenas")) && !deadPlayers.containsKey(e.getPlayer().getName())){
								e.setCancelled(true);
								if (e.getPlayer().getInventory().contains(Material.ARROW) || !getConfig().getBoolean("require-ammo-for-guns")){
									if (getConfig().getBoolean("require-ammo-for-guns")){
										removeArrow(e.getPlayer().getInventory());
										e.getPlayer().updateInventory();
									}
									e.getPlayer().launchProjectile(Arrow.class);
								}
								else
									e.getPlayer().sendMessage(ChatColor.RED + "You need more ammo!");
							}
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e){
		if (joinedPlayers.containsKey(e.getPlayer().getName()) || deadPlayers.containsKey(e.getPlayer().getName()))
			e.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e){
		if (joinedPlayers.containsKey(e.getPlayer().getName()) || deadPlayers.containsKey(e.getPlayer().getName()))
			e.setCancelled(true);
	}

	public void removeArrow(Inventory inv){
		for (int i = 0; i < inv.getContents().length; i++){
			ItemStack is = inv.getItem(i);
			if (is != null){
				if (is.getType() == Material.ARROW){
					if (is.getAmount() == 1)
						inv.setItem(i, null);
					else if (is.getAmount() > 1)
						is.setAmount(is.getAmount() - 1);
					break;
				}
			}
		}
	}
}
