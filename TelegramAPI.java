import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.*;

class InlineQueryResultCachedSticker
{
	String type = "sticker";
	String id = "";
	String sticker_file_id = "";
	
	InlineQueryResultCachedSticker(){}
	
	InlineQueryResultCachedSticker(String id, String sticker_file_id)
	{
		this.id = id;
		this.sticker_file_id = sticker_file_id;
	}
}

class InlineKeyboardButton
{
	String text = "delete";
	InlineKeyboardButton(){}
}

class TelegramAPI 
{
	Gson gson = null;
	
	HttpClient httpClient = HttpClient.newBuilder().build();
	HttpRequest request = null;
	HttpResponse<String> response = null; 
	
	StringBuilder url_str = new StringBuilder();
	String base_url = "https://api.telegram.org/bot";
	String token = "";
	
	TelegramAPI(String token){
		gson = new GsonBuilder().setPrettyPrinting().create();
		this.token = token;
		base_url = base_url + token;
	}
	
	JsonArray getUpdates(int offset) 
	{
		url_str.setLength(0);
		url_str.append(base_url).append("/getUpdates?timeout=120&offset=").append(Integer.toString(offset));
		
		request  = HttpRequest.newBuilder().GET().uri(URI.create(url_str.toString())).build();
		JsonArray updates = null;
		try{
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());			
			
			updates = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("result");
			
			// Skip logging of empty updates
			if(updates.size() != 0){
				System.out.println(java.time.LocalDateTime.now() + " Getting update... Response:");
				System.out.println(gson.toJson(JsonParser.parseString(response.body())));			
			}
			
		} catch (Exception e){
			System.out.println(e);
		}
		
		return updates;
	}
	
	void sendStickerWithInline(String chat_id, String sticker_id, String sticker_index)
	{
		url_str.setLength(0);

		String delete_key  = "{\"inline_keyboard\":[[{\"text\":\"DELETE\",\"callback_data\":\"" + sticker_index + "\"}]]}";
		
		try
		{
			String encoded_key = URLEncoder.encode(delete_key, "UTF-8");
		
			url_str.append(base_url).append("/sendSticker?chat_id=" + chat_id + "&sticker=" + sticker_id + 
																	"&reply_markup=" + encoded_key);
																	
			request  = HttpRequest.newBuilder().GET().uri(URI.create(url_str.toString())).build();
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			System.out.println(java.time.LocalDateTime.now() + " Sending stickers with buttons... Response:");
			System.out.println(gson.toJson(JsonParser.parseString(response.body())));
		} catch (Exception e){
			System.out.println(e);
		}

		return;
	}
	
	
	void sendMessage(String chat_id, String msg)
	{
		url_str.setLength(0);
		
		try
		{
			String encoded_msg = URLEncoder.encode(msg, "UTF-8");
			url_str.append(base_url).append("/sendMessage?chat_id=" + chat_id + "&text=" + encoded_msg);
		
			request  = HttpRequest.newBuilder().GET().uri(URI.create(url_str.toString())).build();
	
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());			

			System.out.println(java.time.LocalDateTime.now() + " Sent a message: " + msg + " Response:");	
			System.out.println(gson.toJson(JsonParser.parseString(response.body())));
		} catch (Exception e){
			System.out.println(e);
		}
				
		return;
	}
	
	void sendInlineAnswer(String query_id, String answer)
	{
		url_str.setLength(0);
	
		try
		{
			String encoded_answer = URLEncoder.encode(answer, "UTF-8");
			url_str.append(base_url).append("/answerInlineQuery?inline_query_id=" + query_id + 
															"&results=" + encoded_answer + "&cache_time=0");
			
			request  = HttpRequest.newBuilder().GET().uri(URI.create(url_str.toString())).build();

			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			System.out.println(gson.toJson(JsonParser.parseString(response.body())));			
		} catch (Exception e){
			System.out.println(e);
		}
	
		return;
	}
	
	void deleteMessage(String chat_id, String msg_id)
	{
		url_str.setLength(0);

		try
		{
			url_str.append(base_url).append("/deleteMessage?chat_id=" + chat_id + "&message_id=" + msg_id);
		
			request  = HttpRequest.newBuilder().GET().uri(URI.create(url_str.toString())).build();
		
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			System.out.println(java.time.LocalDateTime.now() + " Deleting msg, id: " + msg_id + " Response: ");
			System.out.println(gson.toJson(JsonParser.parseString(response.body())));
			
		} catch (Exception e){
			System.out.println(e);
		}
	
		return;
	}
}