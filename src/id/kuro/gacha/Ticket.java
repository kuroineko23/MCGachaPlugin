package id.kuro.gacha;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class Ticket implements Listener
{
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent e)
	{
		FileConfiguration config = Main.getPlugin().getConfig();
		File sql_path = new File(Main.getPlugin().getDataFolder(), config.getString("SQLite Path"));
		LivingEntity entity = e.getEntity();
		if(entity instanceof Monster)
		{
			if(entity.getKiller() instanceof Player)
			{
				Player player = entity.getKiller();
				Integer roll = ThreadLocalRandom.current().nextInt(10000);
				if(roll >= 9997 || entity.getName().contentEquals("Ravager"))
				{
					Boolean updateSuccess = true;
					String sql = 	"UPDATE gacha "
							+		"SET ticket = ticket + 10 "
							+		"WHERE name = ?;";
					try
					{
						Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sql_path);
						PreparedStatement stmt = conn.prepareStatement(sql);
						stmt.setString(1, player.getPlayer().getName());
						stmt.executeUpdate();
						conn.close();
					}
					catch (SQLException error)
					{
						System.out.println("[GachaPlugin] Fail to update data!");
						System.out.println("[GachaPlugin] " + error.getMessage());
						error.printStackTrace();
						updateSuccess = false;
					}
					if(updateSuccess)
					{
						player.sendMessage(ChatColor.GOLD + "You got 10 ticket!");
					}
					else
					{
						player.sendMessage(ChatColor.RED + "An error has occured while updating the database!\nPlease notify the Server Administrator immediately!");
					}
				}
				else if(roll >= 7000)
				{
					//update the database, incrementing ticket by 1
					Boolean updateSuccess = true;
					String sql = 	"UPDATE gacha "
							+		"SET ticket = ticket + 1 "
							+		"WHERE name = ?;";
					try
					{
						Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sql_path);
						PreparedStatement stmt = conn.prepareStatement(sql);
						stmt.setString(1, player.getPlayer().getName());
						stmt.executeUpdate();
						conn.close();
					}
					catch (SQLException error)
					{
						System.out.println("[GachaPlugin] Fail to update data!");
						System.out.println("[GachaPlugin] " + error.getMessage());
						error.printStackTrace();
						updateSuccess = false;
					}
					
					if(updateSuccess)
					{
						player.sendMessage(ChatColor.GREEN + "You got 1 ticket!");
					}
					else
					{
						player.sendMessage(ChatColor.RED + "An error has occured while updating the database!\nPlease notify the Server Administrator immediately!");
					}
				}
				
			}
		}
	}
}
