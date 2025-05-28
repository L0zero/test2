import os
import openai
from openai import OpenAI

client = OpenAI(api_key="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")
# TODO: The 'openai.organization' option isn't read in the client API. You will need to pass it when you instantiate the client, e.g. 'OpenAI(organization="org-fBoqj45hvJisAEGMR5cvPnDS")'
# openai.organization = "org-fBoqj45hvJisAEGMR5cvPnDS"
client.models.list()