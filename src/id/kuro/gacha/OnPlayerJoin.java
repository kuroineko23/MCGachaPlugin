package id.kuro.gacha;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.configuration.file.FileConfiguration;

public class OnPlayerJoin implements Listener
{
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent player)
	{
		FileConfiguration config = Main.getPlugin().getConfig();
		File sql_path = new File(Main.getPlugin().getDataFolder(), config.getString("SQLite Path"));
		
		//insert new data to SQLite
		String sql = "INSERT OR IGNORE INTO gacha(name,ticket) VALUES(?,0);";
		try
		{
			Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sql_path);
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, player.getPlayer().getName());
			stmt.executeUpdate();
			conn.close();
		}
		catch (SQLException e)
		{
			System.out.println("[GachaPlugin] Fail to insert new data!");
			System.out.println("[GachaPlugin] " + e.getMessage());
			e.printStackTrace();
		}
	}
}
