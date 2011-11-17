package gov.va.akcds.splMojo.dataTypes;

import java.util.UUID;

public class DynamicDataType
{
	private String name_;
	private UUID identifier_;
	
	public DynamicDataType(String name, UUID identifier)
	{
		this.name_ = name;
		this.identifier_ = identifier;
	}
	
	public String getName()
	{
		return name_;
	}
	
	public UUID getIdentifier()
	{
		return identifier_;
	}
	
}
