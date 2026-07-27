import urllib.request
import xml.etree.ElementTree as ET
from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer

def get_market_sentiment():
    """
    Fetches the latest news headlines from Economic Times Markets RSS feed,
    analyzes their sentiment using VADER, and returns an aggregate sentiment score
    between -1.0 (Bearish) and 1.0 (Bullish).
    """
    url = 'https://economictimes.indiatimes.com/markets/rssfeeds/1977021501.cms'
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = urllib.request.urlopen(req)
        xml_data = res.read()
        root = ET.fromstring(xml_data)
        
        analyzer = SentimentIntensityAnalyzer()
        scores = []
        
        # Analyze top 20 headlines
        items = root.findall('.//item')[:20]
        if not items:
            return 0.0
            
        for item in items:
            title = item.find('title').text
            if title:
                score = analyzer.polarity_scores(title)['compound']
                scores.append(score)
                
        if not scores:
            return 0.0
            
        # Return the average sentiment score
        avg_score = sum(scores) / len(scores)
        return round(avg_score, 4)
        
    except Exception as e:
        print(f"Error fetching/parsing news sentiment: {e}")
        return 0.0

if __name__ == "__main__":
    score = get_market_sentiment()
    print(f"Current Market News Sentiment: {score}")
