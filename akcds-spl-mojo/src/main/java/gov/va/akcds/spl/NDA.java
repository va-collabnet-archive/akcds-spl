package gov.va.akcds.spl;

public class NDA
{
	private String type_;
	private String value_;
	
	protected NDA(String rawValue)
	{
		int split = 0;
		for (int i = 0; i < rawValue.length(); i++)
		{
			if (Character.isDigit(rawValue.charAt(i)))
			{
				split = i;
				break;
			}
		}
		type_ = rawValue.substring(0, split).toUpperCase();
		value_ = rawValue.substring(split, rawValue.length());
	}

	@Override
	public int hashCode()
	{
		return (type_ + value_).hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof NDA)
		{
			return (type_ + value_).equals(((NDA)obj).type_ + ((NDA)obj).value_);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return type_ + " : " + value_; 
	}

	public String getType()
	{
		return type_;
	}

	public String getValue()
	{
		return value_;
	}
}
