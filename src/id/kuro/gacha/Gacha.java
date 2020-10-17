package id.kuro.gacha;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;

public class Gacha implements CommandExecutor{

	//if player send /gacha command
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		
		if(sender instanceof Player)
		{
			Player player = (Player) sender;
			
			//get data from SQLite
			Integer ticket = null;
			FileConfiguration config = Main.getPlugin().getConfig();
			File sql_path = new File(Main.getPlugin().getDataFolder(), config.getString("SQLite Path"));
			
			{
				String sql = 	"SELECT ticket "
						+		"FROM gacha WHERE name = ?";
				
				Connection conn = null;
				try
				{
					conn = DriverManager.getConnection("jdbc:sqlite:" + sql_path);
					PreparedStatement stmt = conn.prepareStatement(sql);
					stmt.setString(1, player.getName());
					ResultSet rs = stmt.executeQuery();
					ticket = rs.getInt("ticket");
				}
				catch (SQLException e)
				{
					System.out.println("[GachaPlugin] Cannot get data from database!");
					System.out.println("[GachaPlugin] " + e.getMessage());
					e.printStackTrace();
				}
			}
			
			if(args.length == 0)
			{
				player.sendMessage("You have " + ticket + " ticket(s)");
			}
			else if(args[0].matches("[0-9]+") && Integer.parseInt(args[0]) != 0)
			{
				Integer roll = Integer.parseInt(args[0]);
				if(ticket - roll >= 0)
				{
					ticket -= roll;
					//update the database
					Boolean updateStatus = true;
					{
						String sql = 	"UPDATE gacha "
								+		"SET ticket = ? "
								+		"WHERE name = ?;";
						
						Connection conn = null;
						try
						{
							conn = DriverManager.getConnection("jdbc:sqlite:" + sql_path);
							PreparedStatement stmt = conn.prepareStatement(sql);
							stmt.setInt(1, ticket);
							stmt.setString(2, player.getName());
							stmt.executeUpdate();
							conn.close();
						}
						catch (SQLException e)
						{
							System.out.println("[GachaPlugin] Fail to update database!");
							System.out.println("[GachaPlugin] " + e.getMessage());
							e.printStackTrace();
							updateStatus = false;
						}
					}					
					if(updateStatus)
					{
						for(int i = 0; i < roll; i++)
						{
							ItemStack item = null;
							item = Main.getItem();
							player.getInventory().addItem(item);
							player.sendMessage(ChatColor.GREEN + "You got " + item.getAmount() + " " + WordUtils.capitalizeFully(item.getType().toString().replace("_", " ")) + "!");
						}
					}
					else
					{
						player.sendMessage(ChatColor.RED + "An error has occured while updating the database!\nPlease notify the Server Administrator immediately!");
					}
				}
				else
				{
					player.sendMessage(ChatColor.RED + "Not enough ticket(s)!");
				}
				
			}
			else
			{
				player.sendMessage(ChatColor.RED + "Invalid arguments!");
			}
			return true;
		}
		else
		{
			return false;
		}
	}
}