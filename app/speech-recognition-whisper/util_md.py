
from imports import *
import re

def index_md_file(md_file_path: str) -> dict:
    with open(md_file_path, 'r') as file:
        return index_md_lines(file.readlines())

def index_md_text(md_text: str) -> dict:
    return index_md_lines(md_text.splitlines())

def index_md_lines(md_text_lines) -> dict:
    """
    Reads an entire MD document, identifies all headers, and returns a dictionary of header:content.

    Parameters:
    - file_path: Path to the Markdown file.

    Returns:
    A dictionary where keys are unique header paths and values are the text under those headers.
    """
    header_pattern = r"^(#{1,6})\s+(.+)$"
    header_stack = []
    header_dict = {}
    header_path = ''
    content = ""
    for line in md_text_lines:
        match = re.match(header_pattern, line)
        if match:
            # Extracting header level and text
            header_level, header_text = match.groups()
            header_depth = len(header_level) - 1  # Adjusting depth based on number of '#' symbols

            # Update header stack
            header_stack = header_stack[:header_depth-1]  # Truncate the stack to the current depth
            header_stack.append(header_text)
            # Update header_dict with content under the previous header
            if len(header_stack) > 0 and header_path:
                parent_header = " > ".join(header_stack[:-1])
                header_dict[header_path] = content.strip()
                content = ""  # Reset content for the new header

            # Building the header path
            header_path = " > ".join(header_stack)


        else:
            # Accumulating content under the current header
            content += line

    # Finalizing the dictionary by adding the last set of content
    if header_stack:
        header_path = " > ".join(header_stack)
        header_dict[header_path] = content.strip()

    return header_dict