package org.entermediadb.markdown;

import java.util.Objects;

import org.entermediadb.markdown.node.BlockQuote;
import org.entermediadb.markdown.node.BulletList;
import org.entermediadb.markdown.node.Code;
import org.entermediadb.markdown.node.Emphasis;
import org.entermediadb.markdown.node.FencedCodeBlock;
import org.entermediadb.markdown.node.HardLineBreak;
import org.entermediadb.markdown.node.Heading;
import org.entermediadb.markdown.node.HtmlBlock;
import org.entermediadb.markdown.node.HtmlInline;
import org.entermediadb.markdown.node.Image;
import org.entermediadb.markdown.node.IndentedCodeBlock;
import org.entermediadb.markdown.node.Link;
import org.entermediadb.markdown.node.ListItem;
import org.entermediadb.markdown.node.Node;
import org.entermediadb.markdown.node.OrderedList;
import org.entermediadb.markdown.node.Paragraph;
import org.entermediadb.markdown.node.SoftLineBreak;
import org.entermediadb.markdown.node.StrongEmphasis;
import org.entermediadb.markdown.node.Text;
import org.entermediadb.markdown.node.ThematicBreak;
import org.entermediadb.markdown.parser.Parser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Turns markdown into Quill Delta ops, which is what the chat clients render.
 *
 * Inline styles (bold, italic, code, links) become attributes on the text. Block styles (headings,
 * lists, quotes, code blocks) become attributes on the newline that ends the block, as Quill expects.
 * Markdown that is still arriving is fine: an unclosed code fence is a code block to the end of the
 * text, and half a sentence is just a paragraph.
 */
public class MarkdownToQuill
{
	/** Ops for inMarkdown. Empty when there is nothing to show; otherwise the last op is always a newline. */
	public JSONArray toOps(String inMarkdown)
	{
		JSONArray ops = new JSONArray();
		if (inMarkdown == null || inMarkdown.trim().isEmpty())
		{
			return ops;
		}
		Node document = Parser.builder().build().parse(inMarkdown);
		blocks(document, ops, false);
		return ops;
	}

	protected void blocks(Node inParent, JSONArray inOps, boolean inQuote)
	{
		for (Node child = inParent.getFirstChild(); child != null; child = child.getNext())
		{
			if (child instanceof Paragraph)
			{
				inline(child, inOps, null);
				newline(inOps, inQuote ? attributes("blockquote", Boolean.TRUE) : null);
			}
			else if (child instanceof Heading)
			{
				inline(child, inOps, null);
				newline(inOps, attributes("header", Integer.valueOf(Math.max(1, Math.min(6, ((Heading) child).getLevel())))));
			}
			else if (child instanceof BulletList)
			{
				list(child, inOps, "bullet", 0);
			}
			else if (child instanceof OrderedList)
			{
				list(child, inOps, "ordered", 0);
			}
			else if (child instanceof FencedCodeBlock)
			{
				codeBlock(((FencedCodeBlock) child).getLiteral(), inOps);
			}
			else if (child instanceof IndentedCodeBlock)
			{
				codeBlock(((IndentedCodeBlock) child).getLiteral(), inOps);
			}
			else if (child instanceof BlockQuote)
			{
				blocks(child, inOps, true);
			}
			else if (child instanceof ThematicBreak)
			{
				text(inOps, "——————", null);
				newline(inOps, null);
			}
			else if (child instanceof HtmlBlock)
			{
				text(inOps, trimTrailingNewlines(((HtmlBlock) child).getLiteral()), null);
				newline(inOps, null);
			}
			else if (child.getFirstChild() != null)
			{
				blocks(child, inOps, inQuote);
			}
		}
	}

	/** Each item's paragraph ends with a newline marked as a list line; nested lists are indented one level */
	protected void list(Node inList, JSONArray inOps, String inType, int inIndent)
	{
		for (Node item = inList.getFirstChild(); item != null; item = item.getNext())
		{
			if (!(item instanceof ListItem))
			{
				continue;
			}
			for (Node child = item.getFirstChild(); child != null; child = child.getNext())
			{
				if (child instanceof BulletList)
				{
					list(child, inOps, "bullet", inIndent + 1);
				}
				else if (child instanceof OrderedList)
				{
					list(child, inOps, "ordered", inIndent + 1);
				}
				else if (child instanceof Paragraph || child instanceof Heading)
				{
					inline(child, inOps, null);
					JSONObject attributes = attributes("list", inType);
					if (inIndent > 0)
					{
						attributes.put("indent", Integer.valueOf(inIndent));
					}
					newline(inOps, attributes);
				}
				else if (child instanceof FencedCodeBlock)
				{
					codeBlock(((FencedCodeBlock) child).getLiteral(), inOps);
				}
			}
		}
	}

	/** Quill marks every line of a code block with its own newline */
	protected void codeBlock(String inLiteral, JSONArray inOps)
	{
		String code = trimTrailingNewlines(inLiteral);
		for (String line : code.split("\n", -1))
		{
			if (!line.isEmpty())
			{
				text(inOps, line, null);
			}
			newline(inOps, attributes("code-block", Boolean.TRUE));
		}
	}

	protected void inline(Node inParent, JSONArray inOps, JSONObject inAttributes)
	{
		for (Node child = inParent.getFirstChild(); child != null; child = child.getNext())
		{
			if (child instanceof Text)
			{
				text(inOps, ((Text) child).getLiteral(), inAttributes);
			}
			else if (child instanceof StrongEmphasis)
			{
				inline(child, inOps, with(inAttributes, "bold", Boolean.TRUE));
			}
			else if (child instanceof Emphasis)
			{
				inline(child, inOps, with(inAttributes, "italic", Boolean.TRUE));
			}
			else if (child instanceof Code)
			{
				text(inOps, ((Code) child).getLiteral(), with(inAttributes, "code", Boolean.TRUE));
			}
			else if (child instanceof Link)
			{
				inline(child, inOps, with(inAttributes, "link", ((Link) child).getDestination()));
			}
			else if (child instanceof Image)
			{
				// No inline images in chat: show the description as a link to the picture
				inline(child, inOps, with(inAttributes, "link", ((Image) child).getDestination()));
			}
			else if (child instanceof SoftLineBreak || child instanceof HardLineBreak)
			{
				text(inOps, "\n", null);
			}
			else if (child instanceof HtmlInline)
			{
				text(inOps, ((HtmlInline) child).getLiteral(), inAttributes);
			}
			else if (child.getFirstChild() != null)
			{
				inline(child, inOps, inAttributes);
			}
		}
	}

	/** Adds text, joining it to the op before when that has the same style so the ops stay short */
	protected void text(JSONArray inOps, String inText, JSONObject inAttributes)
	{
		if (inText == null || inText.isEmpty())
		{
			return;
		}
		if (!inOps.isEmpty())
		{
			JSONObject last = (JSONObject) inOps.get(inOps.size() - 1);
			Object lastInsert = last.get("insert");
			if (lastInsert instanceof String && !((String) lastInsert).endsWith("\n") && Objects.equals(last.get("attributes"), inAttributes))
			{
				last.put("insert", lastInsert + inText);
				return;
			}
		}
		add(inOps, inText, inAttributes);
	}

	protected void newline(JSONArray inOps, JSONObject inBlockAttributes)
	{
		add(inOps, "\n", inBlockAttributes);
	}

	@SuppressWarnings("unchecked")
	protected void add(JSONArray inOps, String inInsert, JSONObject inAttributes)
	{
		JSONObject op = new JSONObject();
		op.put("insert", inInsert);
		if (inAttributes != null && !inAttributes.isEmpty())
		{
			op.put("attributes", inAttributes);
		}
		inOps.add(op);
	}

	@SuppressWarnings("unchecked")
	protected JSONObject attributes(String inKey, Object inValue)
	{
		JSONObject attributes = new JSONObject();
		attributes.put(inKey, inValue);
		return attributes;
	}

	/** A copy of inAttributes with one more style, so nested styles (bold inside a link) add up */
	@SuppressWarnings("unchecked")
	protected JSONObject with(JSONObject inAttributes, String inKey, Object inValue)
	{
		JSONObject copy = new JSONObject();
		if (inAttributes != null)
		{
			copy.putAll(inAttributes);
		}
		copy.put(inKey, inValue);
		return copy;
	}

	protected String trimTrailingNewlines(String inText)
	{
		if (inText == null)
		{
			return "";
		}
		int end = inText.length();
		while (end > 0 && (inText.charAt(end - 1) == '\n' || inText.charAt(end - 1) == '\r'))
		{
			end--;
		}
		return inText.substring(0, end);
	}
}
