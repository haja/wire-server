-- | Curate native push tokens based on provider feedback reported
-- via SNS events.
module Gundeck.React (onEvent) where

import Imports
import Gundeck.Monad
import Gundeck.Aws.Sns
import System.Logger.Class (msg, val)

import qualified System.Logger.Class       as Log

onEvent :: Event -> Gundeck ()
onEvent _ = Log.warn $ msg (val "onEvent called")

