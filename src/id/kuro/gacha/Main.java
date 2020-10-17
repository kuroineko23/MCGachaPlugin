package id.kuro.gacha;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin
{
	FileConfiguration config = this.getConfig();
	File config_path = new File(getDataFolder(), "config.yml");
	File sql_path = null;
	
	private static Plugin plugin;
	private static ArrayList<ItemStack> itemList = new ArrayList<>();

	
	//when plugin got enabled
	@Override
	public void onEnable()
	{
		plugin = this;
		//register command
		this.getCommand("gacha").setExecutor(new Gacha());
		
		//register event listener
		getServer().getPluginManager().registerEvents(new OnPlayerJoin(), this);
		getServer().getPluginManager().registerEvents(new Ticket(), this);
		
		if(!config_path.exists())
		{
			System.out.println("[GachaPlugin] Creating new config.yml");
			//creating config.yml
			saveDefaultConfig();
		}
		else
		{
			sql_path = new File(getDataFolder(), config.getString("SQLite Path"));
		}
		
		//SQLite connection
		{
			try
			{
				Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sql_path);
				System.out.println("[GachaPlugin] Successfully established SQLite connection!");
				String sql = "pragma journal_mode=wal";
				System.out.println("[GachaPlugin] WAL mode enabled!");
				PreparedStatement stmt = conn.prepareStatement(sql);
				stmt.execute();
				conn.close();
			}
			catch (SQLException e)
			{
				System.out.println("[GachaPlugin] " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		//SQLite table creation
		{
			String sql =	"CREATE TABLE IF NOT EXISTS gacha(\n"
					+		"name	varchar(100)	PRIMARY KEY,\n"
					+		"ticket	integer,\n"
					+		"UNIQUE(name)"
					+		");";
			
			try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sql_path);
					Statement stmt = conn.createStatement())
			{
				stmt.execute(sql);
				conn.close();
			}
			catch (SQLException e)
			{
				System.out.println("Failed to create new table!");
				System.out.println("[GachaPlugin] " + e.getMessage());
				e.printStackTrace();
			}
		}
		
		//adding item to gacha pool		
		File itemList_path = new File(getDataFolder(), config.getString("Itemlist Path"));
		Integer totalRate = config.getInt("N rate") + config.getInt("R rate") + config.getInt("SR rate") + config.getInt("SSR rate");
		if(!itemList_path.exists())
		{
			System.out.println("[GachaPlugin] No item list found");
			System.out.println("[GachaPlugin] Creating default items.txt");
			try {
				itemList_path.createNewFile();
				PrintStream writer = new PrintStream(itemList_path);
				InputStream input = getClass().getResourceAsStream("/id/kuro/gacha/items.txt");
				Scanner readLine = new Scanner(input);
				while(readLine.hasNextLine())
				{
					writer.println(readLine.nextLine());
					writer.flush();
				}
				writer.close();
				readLine.close();
			} catch (IOException e) {
				System.out.println("[GachaPlugin] Fail to create new items.txt");
				e.printStackTrace();
			}
		}
		else if(totalRate != 100)
		{
			System.out.println("[GachaPlugin] Invalid rarity percentage (" + totalRate + ")");
			System.out.println("[GachaPlugin] Creating default items.txt");
			try {
				itemList_path.createNewFile();
				File defaultItems = new File(getDataFolder(), "default_items.txt");
				PrintStream writer = new PrintStream(defaultItems);
				InputStream input = getClass().getResourceAsStream("/id/kuro/gacha/items.txt");
				Scanner readLine = new Scanner(input);
				while(readLine.hasNextLine())
				{
					writer.println(readLine.nextLine());
					writer.flush();
				}
				writer.close();
				readLine.close();
			} catch (IOException e) {
				System.out.println("[GachaPlugin] Fail to create new items.txt");
				e.printStackTrace();
			}
		}
		ArrayList<ItemStack> n = new ArrayList<>();
		ArrayList<ItemStack> r = new ArrayList<>();
		ArrayList<ItemStack> sr = new ArrayList<>();
		ArrayList<ItemStack> ssr = new ArrayList<>();
		Scanner readLine = null;
		try {
			readLine = new Scanner(itemList_path);
		} catch (FileNotFoundException e) {
			System.out.println("[GachaPlugin] File not found!");
		}
		while(readLine.hasNextLine())
		{
			String currentLine = readLine.nextLine();
			if(currentLine.contains("#"))
			{
				continue;
			}
			else
			{
				try {
					String[] items = currentLine.split("/");
					String itemName = items[0];
					Integer qty = Integer.parseInt(items[1]);
					Integer rarity = Integer.parseInt(items[2]);
					//check items validity
					{
						if(Material.getMaterial(itemName) == null)
						{
							System.out.println("[GachaPlugin] Invalid item name");
						}
						if(qty < 1)
						{
							System.out.println("[GachaPlugin] Invalid item quantity");
						}
						if(rarity < 0 || rarity > 3)
						{
							System.out.println("[GachaPlugin] Invalid item rarity");
						}
					}
					ItemStack currentItem = new ItemStack(Material.getMaterial(itemName.toUpperCase()), qty);
					switch(rarity)
					{
						case 0:
							n.add(currentItem);
							break;
						case 1:
							r.add(currentItem);
							break;
						case 2:
							sr.add(currentItem);
							break;
						case 3:
							ssr.add(currentItem);
							break;
					}
				} catch (Exception e) {
					System.out.println("[GachaPlugin] Error while processing item : " + currentLine);
					continue;
				}
			}
		}
		//insert items to pool
		for(int i = 0; i < config.getInt("N rate"); i++)
		{
			itemList.addAll(n);
		}
		for(int i = 0; i < config.getInt("R rate"); i++)
		{
			itemList.addAll(r);
		}
		for(int i = 0; i < config.getInt("SR rate"); i++)
		{
			itemList.addAll(sr);
		}
		for(int i = 0; i < config.getInt("SSR rate"); i++)
		{
			itemList.addAll(ssr);
		}
	}
	
	//when plugin got disabled
	@Override
	public void onDisable()
	{
		
	}
	
	public static Plugin getPlugin()
	{
		return plugin;
	}
	
	public static ItemStack getItem()
	{
		int roll = ThreadLocalRandom.current().nextInt(itemList.size());
		Collections.shuffle(itemList);
		return itemList.get(roll);
	}
}
