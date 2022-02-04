import java.io.*;
import java.util.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Connection;

import com.google.gson.*;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class Tag
{
	public String user_id = "";
	public String tag_name = "";
	public String sticker_id = "";
	
	Tag(String user_id, String tag_name, String sticker_id)
	{
		this.user_id = user_id;
		this.tag_name = tag_name;
		this.sticker_id = sticker_id;	
	}

	Tag(){}
}

class User
{
	public boolean record_tag = false;
	public Tag tag;
	
	User(){
		this.tag = new Tag();
	}
	
	User(String user_id){
		this.tag = new Tag();
		this.tag.user_id = user_id;
	}
}

class Bot
{
	static Connection con = null;
	static Gson gson = null;
	
	public static void insertTag(String user_id, String tag_name, String sticker_id)
	{
		String insert_qr = "insert into tags values(?,?,?)";
		
		try(PreparedStatement stmt = con.prepareStatement(insert_qr)){
			
			stmt.setString(1, user_id);
			stmt.setString(2, tag_name);
			stmt.setString(3, sticker_id);
			
			stmt.executeUpdate();
						
		} catch (Exception e ){
			System.out.println(e);
		}
	}

	public static List<String> selectStcikers(String user_id, String tag_name)
	{
		String select_qr  = "select sticker_id from tags where user_id = ? and tag = ?";
		
		try(PreparedStatement stmt = con.prepareStatement(select_qr)){
			
			stmt.setString(1, user_id);
			stmt.setString(2, tag_name);
			
			List<String> ret = new ArrayList<String>();
			ResultSet rs = stmt.executeQuery();
			
			while(rs.next()){
				ret.add(rs.getString("sticker_id"));
			}
			
			return ret;
			
		} catch (SQLException e){			
			System.out.println(e);
		}
		
		return null;
	}

	public static List<String> selectTagNames(String user_id)
	{
		String select_qr  = "select distinct tag from tags where user_id = ?";
		
		try(PreparedStatement stmt = con.prepareStatement(select_qr)){
			stmt.setString(1, user_id);
			
			ResultSet rs = stmt.executeQuery();
			List<String> ret = new ArrayList<String>();
			
			while(rs.next()){
				ret.add(rs.getString("tag"));
			}
			
			return ret;
			
		} catch (SQLException e){			
			System.out.println(e);
		}
		
		return null;
	}
	
	public static void deleteTag(String user_id, String tag_name)
	{
		String delete_qr  = "delete from tags where user_id = ? and tag = ?";
		
		try(PreparedStatement stmt = con.prepareStatement(delete_qr)){
			
			stmt.setString(1, user_id);
			stmt.setString(2, tag_name);
			
			stmt.executeUpdate();
			
		} catch (Exception e ){			
			System.out.println(e);
		}
	}
	
	public static void deleteSticker(String user_id, String tag_name, String sticker_id)
	{
		String delete_qr  = "delete from tags where user_id = ? and tag = ? and sticker_id = ? ";
		
		try(PreparedStatement stmt = con.prepareStatement(delete_qr)){
			
			stmt.setString(1, user_id);
			stmt.setString(2, tag_name);
			stmt.setString(3, sticker_id);
			
			stmt.executeUpdate();
			
		} catch (Exception e ){			
			System.out.println(e);
		}
	}
	
	public static void main(String[] args) throws InterruptedException
	{
		gson = new GsonBuilder().setPrettyPrinting().create();
		
		String db_url = "";
		String db_user = "";
		String db_pass = "";
		
		String bot_token = "";
		
		// Read XML config file 
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try
		{
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new File("conf.xml"));
			
			NodeList conf = doc.getElementsByTagName("configuration");
	
			for(int i = 0; i < conf.getLength(); ++i)
			{
				Node node = conf.item(i);

				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element el = (Element) node;
					
					db_url= el.getElementsByTagName("url").item(0).getTextContent();
					db_user = el.getElementsByTagName("user").item(0).getTextContent();
					db_pass = el.getElementsByTagName("password").item(0).getTextContent();
					bot_token = el.getElementsByTagName("token").item(0).getTextContent();
				}
			}

			// Loading MySQL driver
			Class.forName("com.mysql.cj.jdbc.Driver");		
			con = DriverManager.getConnection(db_url, db_user, db_pass);
			
		} catch(Exception ex){
			System.out.println(ex);
			return;
		}
		
		// Offset for bot updates
		int offset = 9000;
	
		String user_id = "";
		String sticker_id = "";
		String query_tag = "";
		
		JsonArray updates = null;
		JsonObject message = null;
		JsonObject inline_query = null;
		JsonObject callback_query = null;
		
		TelegramAPI tag_bot = new TelegramAPI(bot_token);
		
		// Map user_id to user state to handle each user separately
		HashMap<String, User> users_state = new HashMap<String, User>();
		
		while(true)
		{
			updates = tag_bot.getUpdates(offset);
			
			if(updates == null ){
				System.out.println("Couldn't get updates!");
				// Maybe our internet connection dropped. Wait for a minute then try again
				Thread.sleep(1000*60);
				continue;
			} else if(updates.size() == 0) continue;
			
			for(int i = 0; i < updates.size(); ++i)
			{	
				offset = updates.get(i).getAsJsonObject().get("update_id").getAsInt() + 1;
				
				// Got a new message!
				if(updates.get(i).getAsJsonObject().get("message") != null)
				{
					message = updates.get(i).getAsJsonObject().get("message").getAsJsonObject();
					user_id = message.get("from").getAsJsonObject().get("id").getAsString();
					
					if(!users_state.containsKey(user_id)){
						users_state.put(user_id, new User(user_id));
					}
					
					if(message.get("text") != null)
					{
						String msg_text = message.get("text").getAsString();
						String chat_id = message.get("chat").getAsJsonObject().get("id").getAsString();
						
						if(msg_text.contains("/tag")){
							String[] param = msg_text.split(" ");
							
							if(param.length > 2 || param.length == 1){
								tag_bot.sendMessage(chat_id, "Wrong number of parametrs to /tag");
								continue;
							}
							
							// We "register" new tag for the current user
							users_state.get(user_id).tag.tag_name = param[1];
							users_state.get(user_id).record_tag =  true;						
						}
						
						// Stop regestering new TAG
						if(msg_text.equals("/exit")){
							users_state.get(user_id).record_tag = false;
							users_state.get(user_id).tag.tag_name = "";
							continue;
						}
						
						// ls commad with params
						if(msg_text.contains("/ls")){
							String[] param = msg_text.split(" ");
							
							// List all tags
							if(param.length == 1)
							{
								StringBuilder msg = new StringBuilder();
			
								List<String> saved_tags = selectTagNames(user_id);
								if(saved_tags != null){
									for(String saved_tag : saved_tags){
										msg.append(saved_tag + " ");	
									}
								}
								
								if(msg.length() != 0){
									tag_bot.sendMessage(chat_id, msg.toString());										
								} else {
									tag_bot.sendMessage(chat_id, "You do no have saved tags");
								}	
							}
							
							// List all stickers associated with the tag and attatch "delete" button to each one
							if(param.length == 2){
								List<String> stickers = selectStcikers(user_id, param[1]);
								if(stickers != null){
									for(int k = 0; k < stickers.size(); ++k){
										tag_bot.sendStickerWithInline(chat_id, stickers.get(k), 
														 param[1] + " " + Integer.toString(k));
									}
								}
							} else if(param.length > 2){
								tag_bot.sendMessage(chat_id, "Wrong number of parametrs to /ls");
								continue;
							}
						}
						
						if(msg_text.contains("/rm")){
							String[] param = msg_text.split(" ");								
							deleteTag(user_id, param[1]);
						}
					}
					
					if(users_state.get(user_id).record_tag)
					{
						if(message.get("sticker") != null && !users_state.get(user_id).tag.equals(""))
						{
							sticker_id = message.get("sticker").getAsJsonObject().get("file_id").getAsString();			
							insertTag(user_id, users_state.get(user_id).tag.tag_name, sticker_id);
						}
					}
				}
			
				// Got new inline query
				if(updates.get(i).getAsJsonObject().get("inline_query") != null)
				{
					inline_query = updates.get(i).getAsJsonObject().get("inline_query").getAsJsonObject();
					query_tag = inline_query.get("query").getAsString();
					user_id = inline_query.get("from").getAsJsonObject().get("id").getAsString();
												
					StringBuilder array_of_sticker = new StringBuilder();
					
					// See if there is any tag that is similar to query
					ArrayList<InlineQueryResultCachedSticker> answer_stickers = 
																new ArrayList<InlineQueryResultCachedSticker>();
					int insw_index = 0;
					
					List<String> saved_tags = selectTagNames(user_id);
					if(query_tag.equals("")) continue;
				
					if(saved_tags != null){
													
						for(String saved_tag : saved_tags)
						{
							if(saved_tag.contains(query_tag))
							{	
								List<String> stickers = selectStcikers(user_id, saved_tag);
							
								if(stickers != null){
									for(String s_id : stickers)
									{								
										InlineQueryResultCachedSticker st_answer =
											new InlineQueryResultCachedSticker(UUID.randomUUID().toString(), s_id);
										
										// Inline answer cannot include more then  50 stickers
										if(insw_index < 50){
											answer_stickers.add(st_answer);
											++insw_index;
										}
									}
								}
							}	
						}
					}
					
					if(answer_stickers.size() != 0)
					{	
						tag_bot.sendInlineAnswer(inline_query.get("id").getAsString(), gson.toJson(answer_stickers));
					}
				}

				// Got new inline keyboard callback
				if(updates.get(i).getAsJsonObject().get("callback_query") != null)
				{
					callback_query = updates.get(i).getAsJsonObject().get("callback_query").getAsJsonObject();
					message = callback_query.get("message").getAsJsonObject();
					user_id = callback_query.get("from").getAsJsonObject().get("id").getAsString();
					
					String chat_id = message.get("chat").getAsJsonObject().get("id").getAsString();
					String msg_id = callback_query.get("message").getAsJsonObject().get("message_id").getAsString();
					String callback_data = callback_query.get("data").getAsString();
					
					String[] ind_tag = callback_data.split(" ");
					
					List<String> stickers = selectStcikers(user_id, ind_tag[0]);
					
					if(stickers.size() == 0) continue;
					
					deleteSticker(user_id, ind_tag[0], stickers.get(Integer.parseInt(ind_tag[1])));
								
					// Delete msg with that sticker
					tag_bot.deleteMessage(chat_id, msg_id);
				}		
			}
		}
	}
} 